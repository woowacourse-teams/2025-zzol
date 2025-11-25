package coffeeshout.room.infra.messaging;

import coffeeshout.global.config.properties.RedisStreamProperties;
import coffeeshout.room.domain.event.RoomJoinEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisStreamCommands.XAddOptions;
import org.springframework.data.redis.connection.stream.Record;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomEnterStreamProducer {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisStreamProperties redisStreamProperties;
    private final ObjectMapper objectMapper;

    public void broadcastEnterRoom(RoomJoinEvent event) {
        log.info("방 입장 이벤트 발송 시작: eventId={}, joinCode={}, guestName={}",
                event.eventId(), event.joinCode(), event.guestName());

        try {
            final String eventJson = objectMapper.writeValueAsString(event);
            final Record<String, String> objectRecord = StreamRecords.newRecord()
                    .in(redisStreamProperties.roomJoinKey())
                    .ofObject(eventJson);

            final var recordId = stringRedisTemplate.opsForStream().add(
                    objectRecord,
                    XAddOptions.maxlen(redisStreamProperties.maxLength()).approximateTrimming(true)
            );

            log.info("방 입장 이벤트 발송 성공: eventId={}, recordId={}, streamKey={}",
                    event.eventId(), recordId, redisStreamProperties.roomJoinKey());
        } catch (JsonProcessingException e){
            log.error("이벤트 직렬화 실패: eventId={}, joinCode={}, guestName={}",
                    event.eventId(), event.joinCode(), event.guestName(), e);
            throw new RuntimeException("RoomJoinEvent 직렬화 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("방 입장 이벤트 발송 실패: eventId={}, joinCode={}, guestName={}",
                    event.eventId(), event.joinCode(), event.guestName(), e);
            throw new RuntimeException("방 입장 이벤트 발송 실패: " + e.getMessage(), e);
        }
    }
}
