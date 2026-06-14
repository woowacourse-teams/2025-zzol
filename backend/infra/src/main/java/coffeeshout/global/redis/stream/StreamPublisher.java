package coffeeshout.global.redis.stream;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.redis.config.RedisStreamProperties;
import coffeeshout.global.redis.config.RedisStreamProperties.StreamConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
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
    private final StreamTracePropagator streamTracePropagator;

    public StreamPublisher(
            StringRedisTemplate stringRedisTemplate,
            RedisStreamProperties redisStreamProperties,
            @Qualifier("redisObjectMapper") ObjectMapper objectMapper,
            StreamTracePropagator streamTracePropagator
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisStreamProperties = redisStreamProperties;
        this.objectMapper = objectMapper;
        this.streamTracePropagator = streamTracePropagator;
    }

    public void publish(StreamKey key, BaseEvent event) {
        publish(key.getRedisKey(), event);
    }

    /**
     * 이벤트를 직렬화하고 현재 트레이스 컨텍스트를 캐리어 필드로 주입해 발행한다.
     */
    public void publish(String redisKey, BaseEvent event) {
        final Map<String, String> fields = new HashMap<>();
        try {
            fields.put(StreamRecordFields.PAYLOAD, objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("직렬화 실패: " + e.getMessage(), e);
        }
        streamTracePropagator.injectCurrentContext(fields);
        publishFields(redisKey, fields);
    }

    /**
     * 이미 직렬화된 페이로드를 명시된 traceparent와 함께 발행한다.
     * <p>
     * Outbox 릴레이처럼 발행 스레드에 원본 트레이스 컨텍스트가 없는 경로에서 사용한다.
     */
    public void publish(String redisKey, String payload, String traceparent) {
        final Map<String, String> fields = new HashMap<>();
        fields.put(StreamRecordFields.PAYLOAD, payload);
        if (traceparent != null && !traceparent.isBlank()) {
            fields.put(StreamRecordFields.TRACEPARENT, traceparent);
        }
        publishFields(redisKey, fields);
    }

    private void publishFields(String redisKey, Map<String, String> fields) {
        if (redisStreamProperties.keys() == null) {
            log.warn("Redis Stream 설정이 없습니다. 이벤트를 발행하지 않습니다: {}", redisKey);
            return;
        }

        if (!redisStreamProperties.keys().containsKey(redisKey)) {
            log.error("존재하지 않는 키입니다: {}. 사용 가능한 키: {}", redisKey, redisStreamProperties.keys().keySet());
            throw new IllegalArgumentException("존재하지 않는 키입니다: " + redisKey);
        }

        final StreamConfig streamConfig = redisStreamProperties.keys().get(redisKey);

        if (redisStreamProperties.commonSettings() == null) {
            log.warn("Redis Stream 공통 설정이 없습니다. 이벤트를 발행하지 않습니다: {}", redisKey);
            return;
        }

        final int maxLength = streamConfig.getMaxLength(redisStreamProperties.commonSettings());

        stringRedisTemplate.opsForStream().add(
                StreamRecords.newRecord().in(redisKey).ofMap(fields),
                XAddOptions.maxlen(maxLength).approximateTrimming(true)
        );
    }
}
