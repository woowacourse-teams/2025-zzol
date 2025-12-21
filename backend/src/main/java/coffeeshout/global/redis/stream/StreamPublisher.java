package coffeeshout.global.redis.stream;

import coffeeshout.global.redis.config.RedisStreamProperties;
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

    public void publish(StreamKey key, BaseEvent event) {
        if (!redisStreamProperties.keys().containsKey(key.getRedisKey())) {
            throw new IllegalArgumentException("존재하지 않는 키입니다: " + key.getRedisKey());
        }

        final var streamConfig = redisStreamProperties.keys().get(key.getRedisKey());
        if (streamConfig == null) {
            return;
        }

        int maxLength = streamConfig.getMaxLength(redisStreamProperties.commonSettings());

        try {
            stringRedisTemplate.opsForStream().add(
                    StreamRecords.newRecord().in(key.getRedisKey()).ofObject(objectMapper.writeValueAsString(event)),
                    XAddOptions.maxlen(maxLength).approximateTrimming(true)
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("직렬화 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            throw e;
        }
    }
}
