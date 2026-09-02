package coffeeshout.global.redis.stream;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.redis.config.RedisStreamProperties;
import coffeeshout.global.redis.config.RedisStreamProperties.StreamConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.RedisStreamCommands.TrimOptions;
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
    private final MeterRegistry meterRegistry;
    // 발행마다 Counter를 새로 만들지 않도록 스트림별로 캐싱한다 (WebSocketMetricService 선례)
    private final Map<String, Counter> publishedCounters = new ConcurrentHashMap<>();

    public StreamPublisher(
            StringRedisTemplate stringRedisTemplate,
            RedisStreamProperties redisStreamProperties,
            @Qualifier("redisObjectMapper") ObjectMapper objectMapper,
            StreamTracePropagator streamTracePropagator,
            MeterRegistry meterRegistry) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisStreamProperties = redisStreamProperties;
        this.objectMapper = objectMapper;
        this.streamTracePropagator = streamTracePropagator;
        this.meterRegistry = meterRegistry;
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
            log.error(
                    "존재하지 않는 키입니다: {}. 사용 가능한 키: {}",
                    redisKey,
                    redisStreamProperties.keys().keySet());
            throw new IllegalArgumentException("존재하지 않는 키입니다: " + redisKey);
        }

        final StreamConfig streamConfig = redisStreamProperties.keys().get(redisKey);

        if (redisStreamProperties.commonSettings() == null) {
            log.warn("Redis Stream 공통 설정이 없습니다. 이벤트를 발행하지 않습니다: {}", redisKey);
            return;
        }

        final int maxLength = streamConfig.getMaxLength(redisStreamProperties.commonSettings());

        stringRedisTemplate
                .opsForStream()
                .add(
                        StreamRecords.newRecord().in(redisKey).ofMap(fields),
                        XAddOptions.trim(TrimOptions.maxLen(maxLength).approximate()));

        publishedCounters
                .computeIfAbsent(redisKey, this::registerPublishedCounter)
                .increment();
    }

    // 발행률만으로는 적체를 알 수 없다. 소비율(redis_stream_e2e_latency_seconds_count)과 짝지어
    // "발행은 되는데 소비가 0"인 상태를 잡는 데 쓴다 (RedisStreamConsumptionStalled, #1744).
    private Counter registerPublishedCounter(String redisKey) {
        return Counter.builder("redis.stream.published")
                .description("Redis Stream에 발행된 메시지 수")
                .tag("stream", redisKey)
                .register(meterRegistry);
    }
}
