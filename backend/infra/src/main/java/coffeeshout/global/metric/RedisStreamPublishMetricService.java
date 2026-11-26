package coffeeshout.global.metric;

import coffeeshout.global.redis.config.RedisStreamProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Redis Stream 발행 지표.
 *
 * <p>발행 수({@code redis.stream.published})는 동기·비동기 발행 모두 XADD가 성공한 뒤에 센다. 소비율
 * ({@code redis_stream_consumed_total})과 짝지어 "발행은 되는데 소비가 0"인 상태를 잡는 데 쓴다
 * (RedisStreamConsumptionStalled, #1744). 실패한 발행을 세면 그 룰의 좌변이 부풀어 오탐이 된다.</p>
 *
 * <p>비동기 발행은 호출 스레드가 XADD를 기다리지 않으므로, 스레드 점유 시간(WebSocket 핸들러 처리 시간)과 XADD
 * 왕복 시간을 나눠서 잰다. 왕복 시간은 응답이 온 때 결과별로 기록한다. 실패 수는 {@code outcome=error} 왕복 시간의
 * count로 본다. Lettuce 요청 큐가 가득 차 보내지 못한 발행도 여기에 실패로 들어간다.</p>
 *
 * <p>발행할 때마다 지표를 새로 찾지 않도록 스트림별 Counter와 Timer를 처음 한 번만 만들어 둔다.</p>
 */
@Component
@RequiredArgsConstructor
public class RedisStreamPublishMetricService {

    public static final String PUBLISHED_METRIC = "redis.stream.published";
    public static final String ROUND_TRIP_METRIC = "redis.stream.publish.async.duration";

    private final MeterRegistry meterRegistry;
    private final RedisStreamProperties redisStreamProperties;
    private final Map<String, Counter> publishedCounters = new ConcurrentHashMap<>();
    private final Map<String, Timer> successTimers = new ConcurrentHashMap<>();
    private final Map<String, Timer> errorTimers = new ConcurrentHashMap<>();

    // 발행 수를 기동 시점에 0으로 만들어 둔다. 첫 발행 때 시계열이 생기면 Prometheus rate()가 그
    // 첫 샘플을 증가로 세지 않아, 기동 후 발행이 한 번뿐인 스트림은 컨슈머가 멎어도 정지 룰이
    // 침묵한다. 발행이 띄엄한 스트림을 잡는 게 그 룰의 목적이다.
    @PostConstruct
    public void registerPublishedCounters() {
        redisStreamProperties.keys().keySet().forEach(this::publishedCounter);
    }

    /**
     * 동기 발행의 XADD가 성공한 뒤에 부른다.
     */
    public void countPublished(String stream) {
        publishedCounter(stream).increment();
    }

    public Timer.Sample startRoundTrip() {
        return Timer.start(meterRegistry);
    }

    /**
     * 비동기 발행의 XADD가 성공한 때 부른다. 왕복 시간을 남기고 발행 수를 올린다.
     */
    public void recordPublished(Timer.Sample roundTrip, String stream) {
        roundTrip.stop(successTimers.computeIfAbsent(stream, key -> roundTripTimer(key, "success")));
        countPublished(stream);
    }

    public void recordFailed(Timer.Sample roundTrip, String stream) {
        roundTrip.stop(errorTimers.computeIfAbsent(stream, key -> roundTripTimer(key, "error")));
    }

    private Counter publishedCounter(String stream) {
        return publishedCounters.computeIfAbsent(
                stream,
                key -> Counter.builder(PUBLISHED_METRIC)
                        .description("Redis Stream에 발행된 메시지 수")
                        .tag("stream", key)
                        .register(meterRegistry));
    }

    private Timer roundTripTimer(String stream, String outcome) {
        return Timer.builder(ROUND_TRIP_METRIC)
                .description("Redis Stream 비동기 발행(XADD)을 넘긴 때부터 응답이 올 때까지 걸린 시간")
                .tags("stream", stream, "outcome", outcome)
                .publishPercentiles(0.95, 0.99)
                .register(meterRegistry);
    }
}
