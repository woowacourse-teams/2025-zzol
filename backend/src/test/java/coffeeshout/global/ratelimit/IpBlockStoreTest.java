package coffeeshout.global.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.fixture.IntegrationTestSupport;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

@DisplayName("IpBlockStore")
class IpBlockStoreTest extends IntegrationTestSupport {

    private static final String IP = "1.2.3.4";
    private static final String ANOTHER_IP = "5.6.7.8";

    @Autowired
    private IpBlockStore ipBlockStore;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Nested
    @DisplayName("isBlocked")
    class isBlocked {

        @Test
        void 차단된_적_없는_IP는_false를_반환한다() {
            assertThat(ipBlockStore.isBlocked(IP)).isFalse();
        }

        @Test
        void blockImmediately_호출_후_true를_반환한다() {
            ipBlockStore.blockImmediately(IP);

            assertThat(ipBlockStore.isBlocked(IP)).isTrue();
        }

        @Test
        void 다른_IP의_차단은_영향을_주지_않는다() {
            ipBlockStore.blockImmediately(IP);

            assertThat(ipBlockStore.isBlocked(ANOTHER_IP)).isFalse();
        }
    }

    @Nested
    @DisplayName("blockImmediately")
    class blockImmediately {

        @Test
        void IP를_즉시_차단하고_24시간_TTL을_설정한다() {
            ipBlockStore.blockImmediately(IP);

            final Long ttlSeconds = stringRedisTemplate.getExpire("block:ip:" + IP);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(ipBlockStore.isBlocked(IP)).isTrue();
                softly.assertThat(ttlSeconds).isGreaterThan(0).isLessThanOrEqualTo(86400L);
            });
        }
    }

    @Nested
    @DisplayName("incrementNotFoundAndBlockIfExceeded")
    class incrementNotFoundAndBlockIfExceeded {

        @Test
        void 첫_번째_호출에서_1시간_TTL을_설정한다() {
            ipBlockStore.incrementNotFoundAndBlockIfExceeded(IP);

            final Long ttlSeconds = stringRedisTemplate.getExpire("block:404:" + IP);
            assertThat(ttlSeconds).isGreaterThan(0).isLessThanOrEqualTo(3600L);
        }

        @Test
        void 임계값_미만이면_차단하지_않는다() {
            for (int i = 0; i < 4; i++) {
                ipBlockStore.incrementNotFoundAndBlockIfExceeded(IP);
            }

            assertThat(ipBlockStore.isBlocked(IP)).isFalse();
        }

        @Test
        void 임계값_5회_도달_시_IP를_차단한다() {
            for (int i = 0; i < 5; i++) {
                ipBlockStore.incrementNotFoundAndBlockIfExceeded(IP);
            }

            assertThat(ipBlockStore.isBlocked(IP)).isTrue();
        }

        @Test
        void 임계값_초과_이후에도_차단_상태가_유지된다() {
            for (int i = 0; i < 7; i++) {
                ipBlockStore.incrementNotFoundAndBlockIfExceeded(IP);
            }

            assertThat(ipBlockStore.isBlocked(IP)).isTrue();
        }
    }
}
