package coffeeshout.global.metric;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Redis Stream 비동기 발행 지표.
 *
 * <p>호출 스레드가 XADD를 기다리지 않으므로, 스레드 점유 시간(WebSocket 핸들러 처리 시간)과 XADD 왕복 시간을
 * 나눠서 잰다. 왕복 시간은 응답이 온 때 결과별로 기록한다. 실패 수는 {@code outcome=error} 왕복 시간의 count로 보고,
 * 상한에 닿아 XADD를 보내지 않고 거절한 수는 따로 센다.</p>
 *
 * <p>왕복 시간은 응답을 받은 Lettuce 이벤트 루프에서 기록한다. 발행할 때마다 Timer를 새로 찾지 않도록
 * 스트림별 Timer를 처음 한 번만 만들어 둔다.</p>
 */
@Component
@RequiredArgsConstructor
public class RedisStreamPublishMetricService {

    public static final String ROUND_TRIP_METRIC = "redis.stream.publish.async.duration";
    public static final String IN_FLIGHT_METRIC = "redis.stream.publish.async.in-flight";
    public static final String REJECTED_METRIC = "redis.stream.publish.async.rejected";

    private final MeterRegistry meterRegistry;
    private final Map<String, Timer> successTimers = new ConcurrentHashMap<>();
    private final Map<String, Timer> errorTimers = new ConcurrentHashMap<>();

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

    public void recordPublished(Timer.Sample roundTrip, String stream) {
        roundTrip.stop(successTimers.computeIfAbsent(stream, key -> roundTripTimer(key, "success")));
    }

    public void recordFailed(Timer.Sample roundTrip, String stream) {
        roundTrip.stop(errorTimers.computeIfAbsent(stream, key -> roundTripTimer(key, "error")));
    }

    /**
     * 응답을 기다리는 발행이 상한에 닿아 XADD를 보내지 않고 거절한 경우.
     */
    public void recordRejected(String stream) {
        meterRegistry.counter(REJECTED_METRIC, "stream", stream).increment();
    }

    private Timer roundTripTimer(String stream, String outcome) {
        return Timer.builder(ROUND_TRIP_METRIC)
                .description("Redis Stream 비동기 발행(XADD)을 넘긴 때부터 응답이 올 때까지 걸린 시간")
                .tags("stream", stream, "outcome", outcome)
                .publishPercentiles(0.95, 0.99)
                .register(meterRegistry);
    }
}
