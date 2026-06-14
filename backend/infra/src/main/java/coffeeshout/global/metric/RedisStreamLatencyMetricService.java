package coffeeshout.global.metric;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.redis.EventTypeName;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Redis Stream 메시지의 End-to-End 지연 시간을 측정한다.
 *
 * <p>BaseEvent.timestamp() (발행 시점) ~ 소비 시점 간의 시간차를 Micrometer Timer로 기록한다.
 * EventDispatcher에서 이벤트 처리 직전에 호출한다.</p>
 *
 * <p>Prometheus 메트릭명: redis_stream_e2e_latency_seconds</p>
 */
@Slf4j
@Component
public class RedisStreamLatencyMetricService {

    private final MeterRegistry meterRegistry;
    private Timer streamLatencyTimer;

    public RedisStreamLatencyMetricService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void initializeMetrics() {
        this.streamLatencyTimer = Timer.builder("redis.stream.e2e.latency")
                .description("Redis Stream 메시지 발행~소비 간 End-to-End 지연 시간")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(meterRegistry);
    }

    /**
     * 이벤트의 발행 타임스탬프와 현재 시각의 차이를 기록한다.
     *
     * @param event 소비된 Redis Stream 이벤트
     */
    public void recordLatency(BaseEvent event) {
        if (streamLatencyTimer == null) {
            return;
        }

        if (event.timestamp() == null) {
            log.warn("이벤트에 timestamp가 없습니다: eventId={}", event.eventId());
            return;
        }

        final Duration latency = Duration.between(event.timestamp(), Instant.now());

        if (latency.isNegative()) {
            log.warn("음수 지연 감지 (clock skew 의심): eventId={}, latency={}ms",
                    event.eventId(), latency.toMillis());
            return;
        }

        streamLatencyTimer.record(latency);

        if (latency.toMillis() > 50) {
            log.warn("Redis Stream 지연 50ms 초과: eventId={}, latency={}ms, eventType={}",
                    event.eventId(), latency.toMillis(), EventTypeName.of(event));
        }
    }
}
