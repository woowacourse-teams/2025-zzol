package coffeeshout.global.websocket;

import coffeeshout.global.websocket.ui.WebSocketResponse;
import coffeeshout.global.websocket.ui.dto.RecoveryMessage;
import coffeeshout.room.domain.JoinCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

/**
 * 웹소켓 메시지 복구 서비스
 * Redis Stream을 활용하여 메시지를 백업하고 복구 기능을 제공합니다.
 */
@Slf4j
@Service
public class GameRecoveryService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final int maxLength;
    private final int streamTtlSeconds;
    private final int dedupTtlSeconds;

    public GameRecoveryService(
            StringRedisTemplate stringRedisTemplate,
            @Qualifier("redisObjectMapper") ObjectMapper objectMapper,
            @Value("${websocket.recovery.max-length}") int maxLength,
            @Value("${websocket.recovery.stream-ttl-seconds}") int streamTtlSeconds,
            @Value("${websocket.recovery.dedup-ttl-seconds}") int dedupTtlSeconds
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.maxLength = maxLength;
        this.streamTtlSeconds = streamTtlSeconds;
        this.dedupTtlSeconds = dedupTtlSeconds;
    }

    public static final String STREAM_KEY_FORMAT = "room:%s:recovery";
    public static final String ID_MAP_KEY_FORMAT = "room:%s:recovery:ids";

    // Lua Script for atomic save with deduplication (HSET 방식)
    // idMapKey: 중복 방지용 (짧은 TTL, 1초)
    // streamKey: 메시지 복구용 (긴 TTL, 1시간)
    private static final String SAVE_SCRIPT = """
            local idMapKey = KEYS[1]
            local streamKey = KEYS[2]
            local messageId = ARGV[1]
            local destination = ARGV[2]
            local payloadJson = ARGV[3]
            local timestamp = ARGV[4]
            local maxLen = tonumber(ARGV[5])
            local streamTtl = tonumber(ARGV[6])
            local dedupTtl = tonumber(ARGV[7])

            -- 중복 체크: 이미 저장된 메시지인지 확인
            local existingStreamId = redis.call('HGET', idMapKey, messageId)
            if existingStreamId then
                return existingStreamId  -- 기존 streamId 반환
            end

            -- Stream에 저장
            local streamId = redis.call('XADD', streamKey, 'MAXLEN', '~', maxLen, '*',
                'destination', destination,
                'payload', payloadJson,
                'timestamp', timestamp
            )

            -- messageId → streamId 매핑 저장 (짧은 TTL로 중복 방지)
            redis.call('HSET', idMapKey, messageId, streamId)
            redis.call('EXPIRE', idMapKey, dedupTtl)

            -- Stream TTL 설정 (처음 생성 시에만)
            if redis.call('TTL', streamKey) == -1 then
                redis.call('EXPIRE', streamKey, streamTtl)
            end

            return streamId
            """;

    public String generateMessageId(String destination, WebSocketResponse<?> response) {
        try {
            final String content = destination +
                    response.success() +
                    objectMapper.writeValueAsString(response.data()) +
                    response.errorMessage();
            return DigestUtils.md5DigestAsHex(content.getBytes(StandardCharsets.UTF_8));
        } catch (JsonProcessingException e) {
            log.error("메시지 ID 생성 실패", e);
            return DigestUtils.md5DigestAsHex((destination + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * 메시지를 Recovery Stream에 저장 (중복 방지)
     *
     * @param joinCode 방 코드
     * @param destination 웹소켓 destination
     * @param response WebSocketResponse (ID 포함)
     * @param messageId Hash 기반 메시지 ID
     * @return Redis Stream Entry ID (예: "1234567890-0"), 중복인 경우에도 기존 streamId 반환
     */
    public String save(String joinCode, String destination, WebSocketResponse<?> response, String messageId) {
        final String streamKey = String.format(STREAM_KEY_FORMAT, joinCode);
        final String idMapKey = String.format(ID_MAP_KEY_FORMAT, joinCode);

        try {
            final String payloadJson = objectMapper.writeValueAsString(response);
            final String timestamp = String.valueOf(System.currentTimeMillis());

            final RedisScript<String> script = RedisScript.of(SAVE_SCRIPT, String.class);

            final String streamId = stringRedisTemplate.execute(
                    script,
                    List.of(idMapKey, streamKey),
                    messageId,
                    destination,
                    payloadJson,
                    timestamp,
                    String.valueOf(maxLength),
                    String.valueOf(streamTtlSeconds),
                    String.valueOf(dedupTtlSeconds)
            );

            log.debug("복구 메시지 저장: joinCode={}, streamId={}, messageId={}", joinCode, streamId, messageId);

            return streamId;

        } catch (JsonProcessingException e) {
            log.error("메시지 직렬화 실패: joinCode={}", joinCode, e);
            return null;
        } catch (Exception e) {
            log.error("복구 메시지 저장 실패: joinCode={}", joinCode, e);
            return null;
        }
    }

    /**
     * lastStreamId 이후의 메시지 조회 (XRANGE 활용)
     *
     * @param joinCode 방 코드
     * @param lastStreamId 클라이언트가 마지막으로 받은 Redis Stream Entry ID (예: "1234567890-0")
     * @return 복구 메시지 리스트
     */
    public List<RecoveryMessage> getMessagesSince(String joinCode, String lastStreamId) {
        final String streamKey = String.format(STREAM_KEY_FORMAT, joinCode);

        try {
            // lastStreamId 이후 메시지만 조회 (exclusive)
            List<MapRecord<String, Object, Object>> records =
                    stringRedisTemplate.opsForStream()
                            .range(streamKey, Range.open(lastStreamId, "+"));

            if (records == null || records.isEmpty()) {
                log.info("복구 메시지 없음: joinCode={}, lastStreamId={}", joinCode, lastStreamId);
                return List.of();
            }

            final List<RecoveryMessage> messages = records.stream()
                    .map(this::deserializeMessage)
                    .filter(Objects::nonNull)
                    .toList();

            log.info("복구 메시지 조회: joinCode={}, lastStreamId={}, count={}", joinCode, lastStreamId, messages.size());
            return messages;

        } catch (Exception e) {
            log.error("복구 메시지 조회 실패: joinCode={}, lastStreamId={}", joinCode, lastStreamId, e);
            throw e;
        }
    }

    /**
     * Recovery Stream 정리 (방 종료 시)
     *
     * @param joinCode 방 코드
     */
    public void cleanup(JoinCode joinCode) {
        final String streamKey = String.format(STREAM_KEY_FORMAT, joinCode.getValue());
        final String idMapKey = String.format(ID_MAP_KEY_FORMAT, joinCode.getValue());

        try {
            Long deleted = stringRedisTemplate.delete(List.of(streamKey, idMapKey));
            log.info("복구 Stream 정리: joinCode={}, deleted={}", joinCode, deleted);
        } catch (Exception e) {
            log.error("복구 Stream 정리 실패: joinCode={}", joinCode, e);
        }
    }

    /**
     * Redis Stream Record를 RecoveryMessage로 변환
     */
    private RecoveryMessage deserializeMessage(MapRecord<String, Object, Object> mapRecord) {
        try {
            final String streamId = mapRecord.getId().getValue();
            final String destination = (String) mapRecord.getValue().get("destination");
            final String payloadJson = (String) mapRecord.getValue().get("payload");
            final String timestamp = (String) mapRecord.getValue().get("timestamp");

            final WebSocketResponse<?> response = objectMapper.readValue(payloadJson, WebSocketResponse.class);

            return new RecoveryMessage(
                    streamId,
                    destination,
                    response,
                    Long.parseLong(timestamp)
            );
        } catch (Exception e) {
            log.error("메시지 역직렬화 실패: recordId={}", mapRecord.getId(), e);
            return null;
        }
    }
}
