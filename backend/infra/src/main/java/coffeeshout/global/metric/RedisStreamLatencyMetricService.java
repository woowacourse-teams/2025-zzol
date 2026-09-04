package coffeeshout.global.metric;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.redis.EventTypeName;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Redis Stream 메시지의 End-to-End 지연 시간을 스트림별로 측정한다.
 *
 * <p>BaseEvent.timestamp() (발행 시점) ~ 소비 시점 간의 시간차를 Micrometer Timer로 기록한다.
 * EventDispatcher에서 이벤트 처리 직전에 호출한다.</p>
 *
 * <p>Prometheus 메트릭명: redis_stream_e2e_latency_seconds (tag: stream)</p>
 *
 * <p>타이머는 첫 소비 때 만든다. 미리 등록하면 트래픽이 없는 스트림도 버킷 수만큼 시계열을
 * 차지한다. 소비가 끊긴 상태를 판정하는 쪽은 이 타이머가 아니라 {@code redis.stream.consumed}
 * 카운터다(#1744).</p>
 *
 * <p>버킷 범위는 5ms~5s로 좁혔다. 스트림 태그를 붙이면 버킷이 스트림 수만큼 늘어나는데 prod 관측
 * p95가 65ms이고 알림 임계가 0.5s라 위아래로 쓰이지 않는 구간이 넓다. 하한보다 빠른 소비는 최저
 * 버킷({@code le="0.005"}) 하나에 뭉쳐 5ms 미만은 분해되지 않는다. 클라이언트 측 분위수
 * ({@code publishPercentiles})는 등록하지 않는다. 알림이 버킷에서 분위수를 계산하고, 인스턴스별로
 * 나온 분위수는 blue/green을 가로질러 합산되지 않는다.</p>
 */
@Slf4j
@Component
public class RedisStreamLatencyMetricService {

    private static final Duration MIN_EXPECTED = Duration.ofMillis(5);
    private static final Duration MAX_EXPECTED = Duration.ofSeconds(5);
    private static final long SLOW_CONSUME_LOG_THRESHOLD_MS = 50;

    private final MeterRegistry meterRegistry;
    private final Map<String, Timer> timers = new ConcurrentHashMap<>();

    public RedisStreamLatencyMetricService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * 이벤트의 발행 타임스탬프와 현재 시각의 차이를 기록한다.
     *
     * @param streamKey 이벤트가 실려 온 스트림 키
     * @param event     소비된 Redis Stream 이벤트
     */
    public void recordLatency(String streamKey, BaseEvent event) {
        if (event.timestamp() == null) {
            log.warn("이벤트에 timestamp가 없습니다: eventId={}", event.eventId());
            return;
        }

        final Duration latency = Duration.between(event.timestamp(), Instant.now());

        if (latency.isNegative()) {
            log.warn("음수 지연 감지 (clock skew 의심): eventId={}, latency={}ms", event.eventId(), latency.toMillis());
            return;
        }

        timers.computeIfAbsent(streamKey, this::registerTimer).record(latency);

        if (latency.toMillis() > SLOW_CONSUME_LOG_THRESHOLD_MS) {
            log.warn(
                    "Redis Stream 지연 {}ms 초과: stream={}, eventId={}, latency={}ms, eventType={}",
                    SLOW_CONSUME_LOG_THRESHOLD_MS,
                    streamKey,
                    event.eventId(),
                    latency.toMillis(),
                    EventTypeName.of(event));
        }
    }

    private Timer registerTimer(String streamKey) {
        return Timer.builder("redis.stream.e2e.latency")
                .description("Redis Stream 메시지 발행~소비 간 End-to-End 지연 시간")
                .tag("stream", streamKey)
                .publishPercentileHistogram()
                .minimumExpectedValue(MIN_EXPECTED)
                .maximumExpectedValue(MAX_EXPECTED)
                .register(meterRegistry);
    }
}
