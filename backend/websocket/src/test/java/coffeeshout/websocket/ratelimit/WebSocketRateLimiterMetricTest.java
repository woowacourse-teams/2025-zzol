package coffeeshout.websocket.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WebSocketRateLimiterMetricTest {

    private static final int MAX_MESSAGES_PER_SECOND = 20;
    private static final Instant BASE_TIME = Instant.parse("2026-01-01T00:00:00Z");

    private TestClock testClock;
    private MeterRegistry meterRegistry;
    private WebSocketRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        testClock = new TestClock(BASE_TIME);
        meterRegistry = new SimpleMeterRegistry();
        rateLimiter = new WebSocketRateLimiter(MAX_MESSAGES_PER_SECOND, testClock, meterRegistry);
    }

    @Test
    void Rate_Limit_초과_시_드롭_카운터가_증가한다() {
        // given
        String sessionId = "session-1";
        for (int i = 0; i < MAX_MESSAGES_PER_SECOND; i++) {
            rateLimiter.tryAcquire(sessionId);
        }

        // when: 21번째 메시지 → 드롭
        rateLimiter.tryAcquire(sessionId);

        // then
        Counter counter = meterRegistry.find("websocket.ratelimit.dropped.total").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void Rate_Limit_이내에서는_드롭_카운터가_증가하지_않는다() {
        // given
        String sessionId = "session-1";

        // when
        for (int i = 0; i < MAX_MESSAGES_PER_SECOND; i++) {
            rateLimiter.tryAcquire(sessionId);
        }

        // then
        Counter counter = meterRegistry.find("websocket.ratelimit.dropped.total").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(0.0);
    }

    @Test
    void 여러_번_초과_시_드롭_카운터가_누적된다() {
        // given
        String sessionId = "session-1";
        for (int i = 0; i < MAX_MESSAGES_PER_SECOND; i++) {
            rateLimiter.tryAcquire(sessionId);
        }

        // when: 3번 초과
        rateLimiter.tryAcquire(sessionId);
        rateLimiter.tryAcquire(sessionId);
        rateLimiter.tryAcquire(sessionId);

        // then
        Counter counter = meterRegistry.find("websocket.ratelimit.dropped.total").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(3.0);
    }

    @Test
    void 윈도우_리셋_후_초과하면_드롭_카운터가_다시_누적된다() {
        // given
        String sessionId = "session-1";
        for (int i = 0; i < MAX_MESSAGES_PER_SECOND; i++) {
            rateLimiter.tryAcquire(sessionId);
        }
        rateLimiter.tryAcquire(sessionId); // 드롭 1회

        // when: 윈도우 리셋 후 다시 초과
        testClock.advance(Duration.ofSeconds(1));
        for (int i = 0; i < MAX_MESSAGES_PER_SECOND; i++) {
            rateLimiter.tryAcquire(sessionId);
        }
        rateLimiter.tryAcquire(sessionId); // 드롭 2회째

        // then
        Counter counter = meterRegistry.find("websocket.ratelimit.dropped.total").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(2.0);
    }

    static class TestClock extends Clock {
        private Instant current;

        TestClock(Instant initial) {
            this.current = initial;
        }

        void advance(Duration duration) {
            current = current.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }
    }
}
