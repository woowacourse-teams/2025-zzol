package coffeeshout.cardgame.infra.messaging;

import coffeeshout.cardgame.domain.event.SelectCardCommandEvent;
import coffeeshout.global.config.properties.RedisStreamProperties;
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
public class CardSelectStreamProducer {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisStreamProperties redisStreamProperties;
    private final ObjectMapper objectMapper;

    public void broadcastCardSelect(SelectCardCommandEvent event) {
        log.info("카드 선택 이벤트 발송 시작: eventId={}, joinCode={}, playerName={}, cardIndex={}",
                event.eventId(), event.joinCode(), event.playerName(), event.cardIndex());

        try {
            final String value = objectMapper.writeValueAsString(event);

            final Record<String, Object> objectRecord = StreamRecords.newRecord()
                    .in(redisStreamProperties.cardGameSelectKey())
                    .ofObject(value);

            final var recordId = stringRedisTemplate.opsForStream().add(
                    objectRecord,
                    XAddOptions.maxlen(redisStreamProperties.maxLength()).approximateTrimming(true)
            );

            log.info("카드 선택 이벤트 발송 성공: eventId={}, recordId={}, streamKey={}",
                    event.eventId(), recordId, redisStreamProperties.cardGameSelectKey());
        } catch (JsonProcessingException e) {
            log.error("이벤트 직렬화 실패: eventId={}, joinCode={}, playerName={}, cardIndex={}",
                    event.eventId(), event.joinCode(), event.playerName(), event.cardIndex(), e);
            throw new RuntimeException("SelectCardCommandEvent 직렬화 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("카드 선택 이벤트 발송 실패: eventId={}, joinCode={}, playerName={}, cardIndex={}",
                    event.eventId(), event.joinCode(), event.playerName(), event.cardIndex(), e);
            throw new RuntimeException("카드 선택 이벤트 발송 실패: " + e.getMessage(), e);
        }
    }
}
