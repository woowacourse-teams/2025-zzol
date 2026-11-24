package coffeeshout.global.metric;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Redis Stream 비동기 발행 지표.
 *
 * <p>호출 스레드가 XADD를 기다리지 않으므로, 스레드 점유 시간(WebSocket 핸들러 처리 시간)과 XADD 왕복 시간을
 * 나눠서 잰다. 왕복 시간은 응답이 온 때 결과별로 기록한다. 실패 수는 {@code outcome=error} 왕복 시간의 count로 본다.
 * Lettuce 요청 큐가 가득 차 보내지 못한 발행도 여기에 실패로 들어간다.</p>
 *
 * <p>왕복 시간은 응답을 받은 Lettuce 이벤트 루프에서 기록한다. 발행할 때마다 Timer를 새로 찾지 않도록
 * 스트림별 Timer를 처음 한 번만 만들어 둔다.</p>
 */
@Component
@RequiredArgsConstructor
public class RedisStreamPublishMetricService {

    public static final String ROUND_TRIP_METRIC = "redis.stream.publish.async.duration";

    private final MeterRegistry meterRegistry;
    private final Map<String, Timer> successTimers = new ConcurrentHashMap<>();
    private final Map<String, Timer> errorTimers = new ConcurrentHashMap<>();

    public Timer.Sample startRoundTrip() {
        return Timer.start(meterRegistry);
    }

    public void recordPublished(Timer.Sample roundTrip, String stream) {
        roundTrip.stop(successTimers.computeIfAbsent(stream, key -> roundTripTimer(key, "success")));
    }

    public void recordFailed(Timer.Sample roundTrip, String stream) {
        roundTrip.stop(errorTimers.computeIfAbsent(stream, key -> roundTripTimer(key, "error")));
    }

    private Timer roundTripTimer(String stream, String outcome) {
        return Timer.builder(ROUND_TRIP_METRIC)
                .description("Redis Stream 비동기 발행(XADD)을 넘긴 때부터 응답이 올 때까지 걸린 시간")
                .tags("stream", stream, "outcome", outcome)
                .publishPercentiles(0.95, 0.99)
                .register(meterRegistry);
    }
}
