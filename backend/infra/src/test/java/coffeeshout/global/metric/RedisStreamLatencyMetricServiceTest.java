package coffeeshout.global.metric;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.support.StubBaseEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RedisStreamLatencyMetricServiceTest {

    private MeterRegistry meterRegistry;
    private RedisStreamLatencyMetricService latencyMetricService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        latencyMetricService = new RedisStreamLatencyMetricService(meterRegistry);
        latencyMetricService.initializeMetrics();
    }

    @Test
    void 정상적인_이벤트의_지연_시간이_기록된다() {
        // given
        BaseEvent event = createEvent(Instant.now().minusMillis(30));

        // when
        latencyMetricService.recordLatency(event);

        // then
        Timer timer = meterRegistry.find("redis.stream.e2e.latency").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isGreaterThan(0);
    }

    @Test
    void timestamp가_null인_이벤트는_기록하지_않는다() {
        // given
        BaseEvent event = createEvent(null);

        // when
        latencyMetricService.recordLatency(event);

        // then
        Timer timer = meterRegistry.find("redis.stream.e2e.latency").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(0);
    }

    @Test
    void 음수_지연은_기록하지_않는다() {
        // given: 미래 시점의 timestamp (clock skew 시뮬레이션)
        BaseEvent event = createEvent(Instant.now().plusSeconds(10));

        // when
        latencyMetricService.recordLatency(event);

        // then
        Timer timer = meterRegistry.find("redis.stream.e2e.latency").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(0);
    }

    @Test
    void 여러_이벤트의_지연_시간이_누적_기록된다() {
        // given
        BaseEvent event1 = createEvent(Instant.now().minusMillis(10));
        BaseEvent event2 = createEvent(Instant.now().minusMillis(20));
        BaseEvent event3 = createEvent(Instant.now().minusMillis(30));

        // when
        latencyMetricService.recordLatency(event1);
        latencyMetricService.recordLatency(event2);
        latencyMetricService.recordLatency(event3);

        // then
        Timer timer = meterRegistry.find("redis.stream.e2e.latency").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(3);
    }

    private BaseEvent createEvent(Instant timestamp) {
        return new StubBaseEvent("test-event-id", timestamp);
    }
}
