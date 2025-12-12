package coffeeshout.global.redis.stream;

import coffeeshout.global.config.properties.RedisStreamProperties;
import coffeeshout.global.redis.BaseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisStreamCommands.XAddOptions;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StreamPublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisStreamProperties redisStreamProperties;
    private final ObjectMapper objectMapper;

    public void broadcast(BaseEvent event, String channel) {
        try {
            stringRedisTemplate.opsForStream().add(
                    StreamRecords.newRecord().in(channel).ofObject(objectMapper.writeValueAsString(event)),
                    XAddOptions.maxlen(redisStreamProperties.maxLength()).approximateTrimming(true)
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("직렬화 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            throw e;
        }
    }
}
