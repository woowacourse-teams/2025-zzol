package coffeeshout.global.websocket.recovery;

import coffeeshout.global.ui.WebSocketResponse;
import coffeeshout.global.websocket.recovery.dto.RecoveryMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
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
@RequiredArgsConstructor
public class GameRecoveryService {

    private final StringRedisTemplate stringRedisTemplate;

    @Qualifier("redisObjectMapper")
    private final ObjectMapper objectMapper;

    private static final String STREAM_KEY_FORMAT = "room:%s:recovery";
    private static final String ID_SET_KEY_FORMAT = "room:%s:recovery:ids";
    private static final int MAX_LENGTH = 1000;  // 최대 1000개 메시지 보관
    private static final int TTL_SECONDS = 3600;  // 1시간

    // Lua Script for atomic save with deduplication
    private static final String SAVE_SCRIPT = """
            local idKey = KEYS[1]
            local streamKey = KEYS[2]
            local messageId = ARGV[1]
            local destination = ARGV[2]
            local payloadJson = ARGV[3]
            local timestamp = ARGV[4]
            local maxLen = tonumber(ARGV[5])
            local ttl = tonumber(ARGV[6])

            -- 중복 체크: SADD는 이미 존재하면 0 반환
            if redis.call('SADD', idKey, messageId) == 0 then
                return nil
            end

            -- TTL 설정 (처음 생성 시에만)
            if redis.call('TTL', idKey) == -1 then
                redis.call('EXPIRE', idKey, ttl)
                redis.call('EXPIRE', streamKey, ttl)
            end

            -- Stream에 저장
            local streamId = redis.call('XADD', streamKey, 'MAXLEN', '~', maxLen, '*',
                'messageId', messageId,
                'destination', destination,
                'payload', payloadJson,
                'timestamp', timestamp
            )

            return streamId
            """;

    /**
     * 메시지 ID 생성 (Hash 기반)
     *
     * @param destination 웹소켓 destination
     * @param response WebSocketResponse 객체
     * @return MD5 hash (소문자 hex)
     */
    public String generateMessageId(String destination, WebSocketResponse<?> response) {
        try {
            // timestamp는 제외하고 destination + payload만 해싱
            String content = destination + ":" + objectMapper.writeValueAsString(response.data());
            return DigestUtils.md5DigestAsHex(content.getBytes(StandardCharsets.UTF_8));
        } catch (JsonProcessingException e) {
            log.error("메시지 ID 생성 실패", e);
            // fallback: timestamp 기반
            return DigestUtils.md5DigestAsHex((destination + System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * 메시지를 Recovery Stream에 저장 (중복 방지)
     *
     * @param joinCode 방 코드
     * @param destination 웹소켓 destination
     * @param response WebSocketResponse (ID 포함)
     * @param messageId Hash 기반 메시지 ID
     * @return Redis Stream Entry ID (예: "1234567890-0"), 중복인 경우 null
     */
    public String save(String joinCode, String destination, WebSocketResponse<?> response, String messageId) {
        String streamKey = String.format(STREAM_KEY_FORMAT, joinCode);
        String idSetKey = String.format(ID_SET_KEY_FORMAT, joinCode);

        try {
            String payloadJson = objectMapper.writeValueAsString(response);
            String timestamp = String.valueOf(System.currentTimeMillis());

            RedisScript<String> script = RedisScript.of(SAVE_SCRIPT, String.class);

            String streamId = stringRedisTemplate.execute(
                    script,
                    List.of(idSetKey, streamKey),
                    messageId,
                    destination,
                    payloadJson,
                    timestamp,
                    String.valueOf(MAX_LENGTH),
                    String.valueOf(TTL_SECONDS)
            );

            if (streamId == null) {
                log.debug("중복 메시지 감지, 저장 스킵: joinCode={}, messageId={}", joinCode, messageId);
            } else {
                log.debug("복구 메시지 저장: joinCode={}, streamId={}, messageId={}", joinCode, streamId, messageId);
            }

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
     * lastId 이후의 메시지 조회
     *
     * @param joinCode 방 코드
     * @param lastId 클라이언트가 마지막으로 받은 메시지 ID (Hash)
     * @return 복구 메시지 리스트
     */
    public List<RecoveryMessage> getMessagesSince(String joinCode, String lastId) {
        String streamKey = String.format(STREAM_KEY_FORMAT, joinCode);

        try {
            // Redis Stream에서 모든 메시지 조회
            List<MapRecord<String, Object, Object>> records =
                    stringRedisTemplate.opsForStream()
                            .range(streamKey, Range.unbounded());

            if (records == null || records.isEmpty()) {
                log.info("복구 메시지 없음: joinCode={}", joinCode);
                return List.of();
            }

            // lastId 이후 메시지 필터링
            boolean foundLastId = false;
            List<RecoveryMessage> messages = new ArrayList<>();

            for (MapRecord<String, Object, Object> record : records) {
                String recordMessageId = (String) record.getValue().get("messageId");

                if (!foundLastId) {
                    if (recordMessageId.equals(lastId)) {
                        foundLastId = true;
                    }
                    continue;  // lastId까지 스킵
                }

                // lastId 이후 메시지 수집
                RecoveryMessage message = deserializeMessage(record);
                if (message != null) {
                    messages.add(message);
                }
            }

            log.info("복구 메시지 조회: joinCode={}, lastId={}, count={}", joinCode, lastId, messages.size());
            return messages;

        } catch (Exception e) {
            log.error("복구 메시지 조회 실패: joinCode={}, lastId={}", joinCode, lastId, e);
            return List.of();
        }
    }

    /**
     * Recovery Stream 정리 (방 종료 시)
     *
     * @param joinCode 방 코드
     */
    public void cleanup(String joinCode) {
        String streamKey = String.format(STREAM_KEY_FORMAT, joinCode);
        String idSetKey = String.format(ID_SET_KEY_FORMAT, joinCode);

        try {
            Long deleted = stringRedisTemplate.delete(List.of(streamKey, idSetKey));
            log.info("복구 Stream 정리: joinCode={}, deleted={}", joinCode, deleted);
        } catch (Exception e) {
            log.error("복구 Stream 정리 실패: joinCode={}", joinCode, e);
        }
    }

    /**
     * Redis Stream Record를 RecoveryMessage로 변환
     */
    private RecoveryMessage deserializeMessage(MapRecord<String, Object, Object> record) {
        try {
            String messageId = (String) record.getValue().get("messageId");
            String destination = (String) record.getValue().get("destination");
            String payloadJson = (String) record.getValue().get("payload");
            String timestamp = (String) record.getValue().get("timestamp");

            WebSocketResponse<?> response = objectMapper.readValue(payloadJson, WebSocketResponse.class);

            return new RecoveryMessage(
                    messageId,
                    destination,
                    response,
                    Long.parseLong(timestamp)
            );
        } catch (Exception e) {
            log.error("메시지 역직렬화 실패: recordId={}", record.getId(), e);
            return null;
        }
    }
}
