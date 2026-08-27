package coffeeshout.user.infra.redis;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.UserModuleServiceTest;
import coffeeshout.user.domain.OAuthCodeEntry;
import coffeeshout.user.domain.TokenPair;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

class RedisOAuthCodeRepositoryTest extends UserModuleServiceTest {

    private static final String KEY_PREFIX = "oauth:code:";
    private static final long TTL_SECONDS = 60L;

    @Autowired
    RedisOAuthCodeRepository redisOAuthCodeRepository;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    TokenPair 토큰_쌍 = new TokenPair("access-token", "refresh-token");

    @Test
    void 인가코드를_저장하면_TTL이_설정된다() {
        redisOAuthCodeRepository.save("code-ttl", 토큰_쌍, true, TTL_SECONDS);

        final Long ttl = stringRedisTemplate.getExpire(KEY_PREFIX + "code-ttl");

        assertThat(ttl).isNotNull().isPositive().isLessThanOrEqualTo(TTL_SECONDS);
    }

    @Test
    void 저장한_인가코드를_조회하면_토큰_쌍과_신규가입_여부를_돌려준다() {
        redisOAuthCodeRepository.save("code-found", 토큰_쌍, true, TTL_SECONDS);

        final OAuthCodeEntry entry =
                redisOAuthCodeRepository.findAndDelete("code-found").orElseThrow();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(entry.tokenPair()).isEqualTo(토큰_쌍);
            softly.assertThat(entry.isNewUser()).isTrue();
        });
    }

    @Test
    void 한번_조회한_인가코드는_삭제되어_다시_조회되지_않는다() {
        redisOAuthCodeRepository.save("code-once", 토큰_쌍, false, TTL_SECONDS);
        redisOAuthCodeRepository.findAndDelete("code-once");

        assertThat(redisOAuthCodeRepository.findAndDelete("code-once")).isEmpty();
    }

    @Test
    void 존재하지_않는_인가코드를_조회하면_빈_값이다() {
        assertThat(redisOAuthCodeRepository.findAndDelete("code-absent")).isEmpty();
    }

    @Test
    void 구분자_형식이_깨진_값이_저장돼_있으면_빈_값이다() {
        stringRedisTemplate.opsForValue().set(KEY_PREFIX + "code-broken", "access-token||refresh-token");

        assertThat(redisOAuthCodeRepository.findAndDelete("code-broken")).isEmpty();
    }
}
