package coffeeshout.global.websocket.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WebSocketRateLimiterTest {

    private static final int MAX_MESSAGES_PER_SECOND = 20;
    private static final Instant BASE_TIME = Instant.parse("2026-01-01T00:00:00Z");

    private TestClock testClock;
    private WebSocketRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        testClock = new TestClock(BASE_TIME);
        rateLimiter = new WebSocketRateLimiter(MAX_MESSAGES_PER_SECOND, testClock);
    }

    @Test
    void 제한_이내의_메시지는_허용한다() {
        // given
        String sessionId = "session-1";

        // when & then
        for (int i = 0; i < MAX_MESSAGES_PER_SECOND; i++) {
            assertThat(rateLimiter.tryAcquire(sessionId))
                    .as("%d번째 메시지는 허용되어야 한다", i + 1)
                    .isTrue();
        }
    }

    @Test
    void 제한을_초과한_메시지는_거부한다() {
        // given
        String sessionId = "session-1";

        // when: 제한까지 채움
        for (int i = 0; i < MAX_MESSAGES_PER_SECOND; i++) {
            rateLimiter.tryAcquire(sessionId);
        }

        // then: 21번째부터 거부
        assertThat(rateLimiter.tryAcquire(sessionId)).isFalse();
        assertThat(rateLimiter.tryAcquire(sessionId)).isFalse();
    }

    @Test
    void 윈도우가_만료되면_카운터가_리셋된다() {
        // given
        String sessionId = "session-1";

        // when: 제한까지 채움
        for (int i = 0; i < MAX_MESSAGES_PER_SECOND; i++) {
            rateLimiter.tryAcquire(sessionId);
        }
        assertThat(rateLimiter.tryAcquire(sessionId)).isFalse();

        // when: 1초 경과
        testClock.advance(Duration.ofSeconds(1));

        // then: 다시 허용됨
        assertThat(rateLimiter.tryAcquire(sessionId)).isTrue();
    }

    @Test
    void 세션별로_독립적으로_카운트한다() {
        // given
        String sessionA = "session-A";
        String sessionB = "session-B";

        // when: A 세션의 제한을 채움
        for (int i = 0; i < MAX_MESSAGES_PER_SECOND; i++) {
            rateLimiter.tryAcquire(sessionA);
        }

        // then: A는 거부, B는 허용
        assertThat(rateLimiter.tryAcquire(sessionA)).isFalse();
        assertThat(rateLimiter.tryAcquire(sessionB)).isTrue();
    }

    @Test
    void 세션_제거_후_새로운_카운터로_시작한다() {
        // given
        String sessionId = "session-1";

        // when: 제한까지 채운 후 세션 제거
        for (int i = 0; i < MAX_MESSAGES_PER_SECOND; i++) {
            rateLimiter.tryAcquire(sessionId);
        }
        assertThat(rateLimiter.tryAcquire(sessionId)).isFalse();

        rateLimiter.removeSession(sessionId);

        // then: 새 카운터로 다시 허용
        assertThat(rateLimiter.tryAcquire(sessionId)).isTrue();
    }

    @Test
    void 비활성_세션이_정리된다() {
        // given
        rateLimiter.tryAcquire("active-session");
        rateLimiter.tryAcquire("inactive-session");
        assertThat(rateLimiter.getTrackedSessionCount()).isEqualTo(2);

        // when: 31초 경과 후 cleanup
        testClock.advance(Duration.ofSeconds(31));

        // active-session만 다시 접근
        rateLimiter.tryAcquire("active-session");
        rateLimiter.cleanupInactiveSessions();

        // then: inactive-session만 제거됨
        assertThat(rateLimiter.getTrackedSessionCount()).isEqualTo(1);
    }

    /**
     * 테스트용 Clock. 시간을 수동으로 전진시킬 수 있다.
     */
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
