package coffeeshout.global.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.global.ServiceTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ReportRateLimitStoreTest extends ServiceTest {

    @Autowired
    private ReportRateLimitStore rateLimitStore;

    private static final String TEST_IP = "1.2.3.4";
    private static final String ANOTHER_IP = "5.6.7.8";

    @Nested
    @DisplayName("tryAcquire")
    class TryAcquire {

        @Test
        void 임계값_이내_요청은_토큰을_획득한다() {
            for (int i = 0; i < 5; i++) {
                assertThat(rateLimitStore.tryAcquire(TEST_IP)).isTrue();
            }
        }

        @Test
        void 임계값_초과_요청은_토큰_획득에_실패한다() {
            for (int i = 0; i < 5; i++) {
                rateLimitStore.tryAcquire(TEST_IP);
            }

            assertThat(rateLimitStore.tryAcquire(TEST_IP)).isFalse();
        }

        @Test
        void 다른_IP는_카운트가_별도로_관리된다() {
            for (int i = 0; i < 5; i++) {
                rateLimitStore.tryAcquire(TEST_IP);
            }

            assertThat(rateLimitStore.tryAcquire(ANOTHER_IP)).isTrue();
        }
    }
}
