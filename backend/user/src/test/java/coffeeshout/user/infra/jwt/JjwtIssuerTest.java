package coffeeshout.user.infra.jwt;

import static coffeeshout.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.user.config.JwtProperties;
import coffeeshout.user.domain.AuthenticatedUser;
import coffeeshout.user.exception.UserErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JjwtIssuerTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-256-bits-long-for-hs256";
    private JjwtIssuer jwtIssuer;

    @BeforeEach
    void setUp() {
        final JwtProperties properties = new JwtProperties(SECRET, 1800L, 1209600L);
        jwtIssuer = new JjwtIssuer(properties);
    }

    @Nested
    class 토큰_발급 {

        @Test
        void 인증된_사용자_정보로_액세스_토큰을_발급한다() {
            final AuthenticatedUser user = new AuthenticatedUser(1L, "ABCDF");

            final String token = jwtIssuer.issue(user);

            assertThat(token).isNotBlank();
        }
    }

    @Nested
    class 토큰_검증 {

        @Test
        void 유효한_토큰에서_사용자_정보를_복원한다() {
            final AuthenticatedUser original = new AuthenticatedUser(42L, "GHJKL");
            final String token = jwtIssuer.issue(original);

            final AuthenticatedUser result = jwtIssuer.verify(token);

            assertThat(result.userId()).isEqualTo(42L);
            assertThat(result.userCode()).isEqualTo("GHJKL");
        }

        @Test
        void 만료된_토큰은_예외가_발생한다() {
            final JwtProperties expiredProps = new JwtProperties(SECRET, 0L, 1209600L);
            final JjwtIssuer expiredIssuer = new JjwtIssuer(expiredProps);
            final String expiredToken = expiredIssuer.issue(new AuthenticatedUser(1L, "ABCDF"));

            assertCoffeeShoutException(
                    () -> jwtIssuer.verify(expiredToken),
                    UserErrorCode.TOKEN_EXPIRED
            );
        }

        @Test
        void 위변조된_토큰은_예외가_발생한다() {
            assertCoffeeShoutException(
                    () -> jwtIssuer.verify("invalid.jwt.token"),
                    UserErrorCode.INVALID_TOKEN
            );
        }

        @Test
        void 다른_시크릿으로_발급한_토큰은_예외가_발생한다() {
            final JwtProperties otherProps = new JwtProperties(
                    "other-secret-key-must-be-at-least-256-bits-long-for-hs256", 1800L, 1209600L);
            final JjwtIssuer otherIssuer = new JjwtIssuer(otherProps);
            final String tokenFromOther = otherIssuer.issue(new AuthenticatedUser(1L, "ABCDF"));

            assertCoffeeShoutException(
                    () -> jwtIssuer.verify(tokenFromOther),
                    UserErrorCode.INVALID_TOKEN
            );
        }
    }
}
