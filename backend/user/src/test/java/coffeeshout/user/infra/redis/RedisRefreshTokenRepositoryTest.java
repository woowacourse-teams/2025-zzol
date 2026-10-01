package coffeeshout.user.infra.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.UserModuleServiceTest;
import coffeeshout.user.domain.AuthenticatedUser;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

class RedisRefreshTokenRepositoryTest extends UserModuleServiceTest {

    private static final String TOKEN_KEY_PREFIX = "refresh:token:";
    private static final String USER_TOKENS_KEY_PREFIX = "refresh:user:";
    private static final long EXPIRATION_SECONDS = 60L;
    private static final Long 사용자_아이디 = 1L;
    private static final String 사용자_코드 = "AB3CD";

    @Autowired
    RedisRefreshTokenRepository redisRefreshTokenRepository;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Test
    void 토큰과_사용자_토큰_목록_양쪽에_TTL이_설정된다() {
        redisRefreshTokenRepository.save(사용자_아이디, 사용자_코드, "token-ttl", EXPIRATION_SECONDS);

        final Long 토큰_TTL = stringRedisTemplate.getExpire(TOKEN_KEY_PREFIX + "token-ttl");
        final Long 목록_TTL = stringRedisTemplate.getExpire(USER_TOKENS_KEY_PREFIX + 사용자_아이디);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(토큰_TTL).isNotNull().isPositive().isLessThanOrEqualTo(EXPIRATION_SECONDS);
            softly.assertThat(목록_TTL).isNotNull().isPositive().isLessThanOrEqualTo(EXPIRATION_SECONDS);
        });
    }

    @Test
    void 만료시간이_0이면_저장하지_않고_예외를_던진다() {
        // @Repository 예외 변환이 IllegalArgumentException을 DataAccessException으로 감싼다.
        assertThatThrownBy(() -> redisRefreshTokenRepository.save(사용자_아이디, 사용자_코드, "token-zero-ttl", 0L))
                .hasRootCauseInstanceOf(IllegalArgumentException.class);

        assertThat(stringRedisTemplate.hasKey(TOKEN_KEY_PREFIX + "token-zero-ttl"))
                .isFalse();
    }

    @Test
    void 저장한_토큰으로_사용자를_조회한다() {
        redisRefreshTokenRepository.save(사용자_아이디, 사용자_코드, "token-found", EXPIRATION_SECONDS);

        final AuthenticatedUser 조회된_사용자 =
                redisRefreshTokenRepository.findByTokenId("token-found").orElseThrow();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(조회된_사용자.userId()).isEqualTo(사용자_아이디);
            softly.assertThat(조회된_사용자.userCode()).isEqualTo(사용자_코드);
        });
    }

    @Test
    void 존재하지_않는_토큰을_조회하면_빈_값이다() {
        assertThat(redisRefreshTokenRepository.findByTokenId("token-absent")).isEmpty();
    }

    @Test
    void 삭제한_토큰은_조회되지_않는다() {
        redisRefreshTokenRepository.save(사용자_아이디, 사용자_코드, "token-deleted", EXPIRATION_SECONDS);

        redisRefreshTokenRepository.delete("token-deleted");

        assertThat(redisRefreshTokenRepository.findByTokenId("token-deleted")).isEmpty();
    }

    @Test
    void 사용자의_토큰을_모두_삭제하면_토큰과_목록이_함께_사라진다() {
        redisRefreshTokenRepository.save(사용자_아이디, 사용자_코드, "token-first", EXPIRATION_SECONDS);
        redisRefreshTokenRepository.save(사용자_아이디, 사용자_코드, "token-second", EXPIRATION_SECONDS);

        redisRefreshTokenRepository.deleteAllByUserId(사용자_아이디);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(redisRefreshTokenRepository.findByTokenId("token-first"))
                    .isEmpty();
            softly.assertThat(redisRefreshTokenRepository.findByTokenId("token-second"))
                    .isEmpty();
            softly.assertThat(stringRedisTemplate.hasKey(USER_TOKENS_KEY_PREFIX + 사용자_아이디))
                    .isFalse();
        });
    }

    @Test
    void 저장된_토큰이_없는_사용자를_전체_삭제해도_예외가_나지_않는다() {
        assertThatCode(() -> redisRefreshTokenRepository.deleteAllByUserId(사용자_아이디))
                .doesNotThrowAnyException();
    }
}
