package coffeeshout.user.application.service;

import static coffeeshout.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.fixture.UserFixture;
import coffeeshout.ServiceTest;
import coffeeshout.user.domain.TokenPair;
import coffeeshout.user.domain.AuthenticatedUser;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.repository.RefreshTokenRepository;
import coffeeshout.user.domain.repository.UserRepository;
import coffeeshout.user.exception.UserErrorCode;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AuthTokenServiceTest extends ServiceTest {

    @Autowired
    AuthTokenService authTokenService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    User 저장된_엠제이;

    @BeforeEach
    void setUp() {
        저장된_엠제이 = userRepository.save(UserFixture.회원_엠제이());
    }

    @Nested
    class 토큰_발급 {

        @Test
        void accessToken과_refreshToken_쌍을_발급한다() {
            final TokenPair tokens = authTokenService.issue(저장된_엠제이);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(tokens.accessToken()).isNotBlank();
                softly.assertThat(tokens.refreshToken()).isNotBlank();
            });
        }

        @Test
        void 발급된_accessToken으로_사용자_정보를_복원할_수_있다() {
            final TokenPair tokens = authTokenService.issue(저장된_엠제이);

            final AuthenticatedUser verified = authTokenService.verify(tokens.accessToken());

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(verified.userId()).isEqualTo(저장된_엠제이.getId());
                softly.assertThat(verified.userCode()).isEqualTo(저장된_엠제이.getUserCode().value());
            });
        }

        @Test
        void 발급된_refreshToken이_Redis에_저장된다() {
            final TokenPair tokens = authTokenService.issue(저장된_엠제이);
            final String tokenId = extractTokenId(tokens.refreshToken());

            final AuthenticatedUser stored = refreshTokenRepository.findByTokenId(tokenId)
                    .orElseThrow();

            assertThat(stored.userId()).isEqualTo(저장된_엠제이.getId());
        }
    }

    @Nested
    class 토큰_회전 {

        @Test
        void refreshToken으로_새_토큰_쌍을_발급한다() {
            final TokenPair original = authTokenService.issue(저장된_엠제이);

            final TokenPair rotated = authTokenService.rotate(original.refreshToken());

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(rotated.accessToken()).isNotBlank();
                softly.assertThat(rotated.refreshToken()).isNotBlank();
                softly.assertThat(rotated.refreshToken()).isNotEqualTo(original.refreshToken());
            });
        }

        @Test
        void 회전_후_기존_refreshToken은_사용할_수_없다() {
            final TokenPair original = authTokenService.issue(저장된_엠제이);

            authTokenService.rotate(original.refreshToken());

            assertCoffeeShoutException(
                    () -> authTokenService.rotate(original.refreshToken()),
                    UserErrorCode.REFRESH_TOKEN_NOT_FOUND
            );
        }

        @Test
        void 형식이_잘못된_refreshToken으로_회전하면_예외가_발생한다() {
            assertCoffeeShoutException(
                    () -> authTokenService.rotate("invalid-format-without-colon"),
                    UserErrorCode.REFRESH_TOKEN_NOT_FOUND
            );
        }

        @Test
        void 재사용된_refreshToken으로_회전_시도하면_해당_유저의_모든_토큰이_폐기된다() {
            final TokenPair original = authTokenService.issue(저장된_엠제이);
            final TokenPair second = authTokenService.issue(저장된_엠제이);
            authTokenService.rotate(original.refreshToken());

            assertCoffeeShoutException(
                    () -> authTokenService.rotate(original.refreshToken()),
                    UserErrorCode.REFRESH_TOKEN_NOT_FOUND
            );

            final String secondTokenId = extractTokenId(second.refreshToken());
            assertThat(refreshTokenRepository.findByTokenId(secondTokenId)).isEmpty();
        }
    }

    @Nested
    class 토큰_폐기 {

        @Test
        void 사용자의_모든_refreshToken이_삭제된다() {
            final TokenPair first = authTokenService.issue(저장된_엠제이);
            final TokenPair second = authTokenService.issue(저장된_엠제이);

            authTokenService.revoke(저장된_엠제이.getId());

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(refreshTokenRepository.findByTokenId(extractTokenId(first.refreshToken()))).isEmpty();
                softly.assertThat(refreshTokenRepository.findByTokenId(extractTokenId(second.refreshToken()))).isEmpty();
            });
        }
    }

    private String extractTokenId(String refreshToken) {
        return refreshToken.split(":", 2)[1];
    }
}
