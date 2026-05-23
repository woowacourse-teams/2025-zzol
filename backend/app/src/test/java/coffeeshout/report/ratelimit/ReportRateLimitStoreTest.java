package coffeeshout.report.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import coffeeshout.support.ServiceTest;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;

class ReportRateLimitStoreTest extends ServiceTest {

    @Autowired
    private ReportRateLimitStore rateLimitStore;

    @Autowired
    private ReportRateLimitProperties rateLimitProperties;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private RedissonClient redissonClient;

    private static final String KEY_PREFIX = "report:submit:";
    private static final String TEST_IP = "1.2.3.4";
    private static final String ANOTHER_IP = "5.6.7.8";

    @Nested
    @DisplayName("tryAcquire")
    class TryAcquire {

        @Test
        void 임계값_이내_요청은_토큰을_획득한다() {
            for (int i = 0; i < rateLimitProperties.rate(); i++) {
                assertThat(rateLimitStore.tryAcquire(TEST_IP)).isTrue();
            }
        }

        @Test
        void 임계값_초과_요청은_토큰_획득에_실패한다() {
            for (int i = 0; i < rateLimitProperties.rate(); i++) {
                rateLimitStore.tryAcquire(TEST_IP);
            }

            assertThat(rateLimitStore.tryAcquire(TEST_IP)).isFalse();
        }

        @Test
        void 다른_IP는_카운트가_별도로_관리된다() {
            for (int i = 0; i < rateLimitProperties.rate(); i++) {
                rateLimitStore.tryAcquire(TEST_IP);
            }

            assertThat(rateLimitStore.tryAcquire(ANOTHER_IP)).isTrue();
        }
    }

    @Nested
    @DisplayName("메트릭")
    class 메트릭 {

        @Test
        void Rate_Limit_초과_시_dropped_카운터가_증가한다() {
            String ip = "10.0.0.1";
            for (int i = 0; i < rateLimitProperties.rate(); i++) {
                rateLimitStore.tryAcquire(ip);
            }
            double before = meterRegistry.find("report.ratelimit.dropped.total").counter().count();

            rateLimitStore.tryAcquire(ip);

            assertThat(meterRegistry.find("report.ratelimit.dropped.total").counter().count() - before)
                    .isEqualTo(1.0);
        }

        @Test
        void Rate_Limit_이내에서는_dropped_카운터가_증가하지_않는다() {
            String ip = "10.0.0.2";
            double before = meterRegistry.find("report.ratelimit.dropped.total").counter().count();

            rateLimitStore.tryAcquire(ip);

            assertThat(meterRegistry.find("report.ratelimit.dropped.total").counter().count() - before)
                    .isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("TTL")
    class TTL {

        @Test
        void 신규_RateLimiter_생성_시_TTL이_설정된다() {
            String ip = "172.16.0.1";
            rateLimitStore.tryAcquire(ip);

            long remainTimeToLive = redissonClient.getRateLimiter(KEY_PREFIX + ip).remainTimeToLive();
            assertThat(remainTimeToLive).isGreaterThan(0);
        }

        @Test
        void TTL_만료_후_Rate_Limit이_초기화된다() {
            String ip = "172.16.0.2";
            for (int i = 0; i < rateLimitProperties.rate(); i++) {
                rateLimitStore.tryAcquire(ip);
            }
            assertThat(rateLimitStore.tryAcquire(ip)).isFalse();

            await().atMost(Duration.ofSeconds(3))
                    .until(() -> rateLimitStore.tryAcquire(ip));
        }
    }
}
