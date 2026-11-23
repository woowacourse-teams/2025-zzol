package coffeeshout.global.metric;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.Semaphore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Redis Stream 비동기 발행 지표.
 *
 * <p>호출 스레드가 XADD를 기다리지 않으므로, 스레드 점유 시간(WebSocket 핸들러 처리 시간)과 XADD 왕복 시간을
 * 나눠서 잰다. 왕복 시간은 응답이 온 때 기록하고, 응답을 기다리는 발행 수와 실패 수를 함께 남긴다.</p>
 */
@Component
@RequiredArgsConstructor
public class RedisStreamPublishMetricService {

    public static final String ROUND_TRIP_METRIC = "redis.stream.publish.async.duration";
    public static final String IN_FLIGHT_METRIC = "redis.stream.publish.async.in-flight";
    public static final String FAILURE_METRIC = "redis.stream.publish.async.failed";

    private final MeterRegistry meterRegistry;

    /**
     * 응답을 기다리는 발행 수를 상한에서 남은 자리를 빼서 보여 준다.
     */
    public void bindInFlight(Semaphore inFlight, int maxInFlight) {
        Gauge.builder(IN_FLIGHT_METRIC, inFlight, permits -> maxInFlight - permits.availablePermits())
                .description("응답을 기다리는 Redis Stream 비동기 발행 수")
                .register(meterRegistry);
    }

    public Timer.Sample startRoundTrip() {
        return Timer.start(meterRegistry);
    }

    public void recordRoundTrip(Timer.Sample roundTrip, String stream, String outcome) {
        roundTrip.stop(Timer.builder(ROUND_TRIP_METRIC)
                .description("Redis Stream 비동기 발행(XADD)을 넘긴 때부터 응답이 올 때까지 걸린 시간")
                .tags("stream", stream, "outcome", outcome)
                .publishPercentiles(0.95, 0.99)
                .register(meterRegistry));
    }

    public void recordFailure(String stream, String reason) {
        meterRegistry.counter(FAILURE_METRIC, "stream", stream, "reason", reason).increment();
    }
}
