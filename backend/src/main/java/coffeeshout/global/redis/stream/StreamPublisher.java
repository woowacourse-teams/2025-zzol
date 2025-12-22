package coffeeshout.global.redis.stream;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.redis.config.RedisStreamProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.RedisStreamCommands.XAddOptions;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StreamPublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisStreamProperties redisStreamProperties;
    private final ObjectMapper objectMapper;

    public StreamPublisher(
            StringRedisTemplate stringRedisTemplate,
            RedisStreamProperties redisStreamProperties,
            @Qualifier("redisObjectMapper") ObjectMapper objectMapper
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisStreamProperties = redisStreamProperties;
        this.objectMapper = objectMapper;
    }

    public void publish(StreamKey key, BaseEvent event) {
        if (redisStreamProperties.keys() == null) {
            log.warn("Redis Stream 설정이 없습니다. 이벤트를 발행하지 않습니다: {}", key.getRedisKey());
            return;
        }

        if (!redisStreamProperties.keys().containsKey(key.getRedisKey())) {
            log.error("존재하지 않는 키입니다: {}. 사용 가능한 키: {}", key.getRedisKey(), redisStreamProperties.keys().keySet());
            throw new IllegalArgumentException("존재하지 않는 키입니다: " + key.getRedisKey());
        }

        final var streamConfig = redisStreamProperties.keys().get(key.getRedisKey());

        if (redisStreamProperties.commonSettings() == null) {
            log.warn("Redis Stream 공통 설정이 없습니다. 이벤트를 발행하지 않습니다: {}", key.getRedisKey());
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
        }
    }
}
