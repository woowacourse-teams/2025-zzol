package coffeeshout.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import coffeeshout.UserModuleServiceTest;
import coffeeshout.global.nickname.ProfanityChecker;
import coffeeshout.user.domain.OAuthProvider;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.UserNickname;
import coffeeshout.user.domain.repository.UserRepository;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserRegistrationServiceTest extends UserModuleServiceTest {

    @Autowired
    UserRegistrationService userRegistrationService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ProfanityChecker profanityChecker;

    private static final OAuthProvider GOOGLE = OAuthProvider.GOOGLE;
    private static final String PROVIDER_USER_ID = "google-uid-42";
    private static final String EMAIL = "test@example.com";

    @Nested
    class 신규_가입 {

        @Test
        void 새_User가_생성되고_UserCode가_부여된다() {
            final LoginResult result = userRegistrationService.registerOrLogin(
                    GOOGLE, PROVIDER_USER_ID, EMAIL, "용감한호랑이");
            final User user = result.user();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(user.getId()).isNotNull();
                softly.assertThat(user.getUserCode().value()).hasSize(5);
                softly.assertThat(user.getNickname().value()).isEqualTo("용감한호랑이");
            });
        }

        @Test
        void 신규_가입이면_isNewUser가_true다() {
            final LoginResult result = userRegistrationService.registerOrLogin(
                    GOOGLE, PROVIDER_USER_ID, EMAIL, "용감한호랑이");

            assertThat(result.isNewUser()).isTrue();
        }

        @Test
        void 닉네임이_null이면_자동_닉네임으로_가입된다() {
            final User user = userRegistrationService.registerOrLogin(
                    GOOGLE, PROVIDER_USER_ID, EMAIL, null).user();

            assertThat(user.getNickname().value())
                    .isNotBlank()
                    .hasSizeLessThanOrEqualTo(UserNickname.MAX_LENGTH);
        }

        @Test
        void 닉네임이_빈_문자열이면_자동_닉네임으로_가입된다() {
            final User user = userRegistrationService.registerOrLogin(
                    GOOGLE, PROVIDER_USER_ID, EMAIL, "").user();

            assertThat(user.getNickname().value())
                    .isNotBlank()
                    .hasSizeLessThanOrEqualTo(UserNickname.MAX_LENGTH);
        }

        @Test
        void 비속어_닉네임이면_자동_닉네임으로_대체된다() {
            given(profanityChecker.contains("씨발")).willReturn(true);

            final User user = userRegistrationService.registerOrLogin(
                    GOOGLE, PROVIDER_USER_ID, EMAIL, "씨발").user();

            assertThat(user.getNickname().value()).isNotEqualTo("씨발");
        }

        @Test
        void 닉네임이_최대_길이를_초과하면_잘라서_사용한다() {
            final User user = userRegistrationService.registerOrLogin(
                    GOOGLE, PROVIDER_USER_ID, EMAIL, "용감한호랑이열한글자초과").user();

            assertThat(user.getNickname().value().length())
                    .isLessThanOrEqualTo(UserNickname.MAX_LENGTH);
        }
    }

    @Nested
    class 기존_회원_로그인 {

        @Test
        void 동일_provider와_providerUserId이면_기존_User를_반환한다() {
            final User first = userRegistrationService.registerOrLogin(
                    GOOGLE, PROVIDER_USER_ID, EMAIL, "처음닉네임").user();

            final User second = userRegistrationService.registerOrLogin(
                    GOOGLE, PROVIDER_USER_ID, EMAIL, "다른닉네임").user();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(second.getId()).isEqualTo(first.getId());
                softly.assertThat(second.getNickname().value()).isEqualTo("처음닉네임");
            });
        }

        @Test
        void 기존_회원_로그인이면_isNewUser가_false다() {
            userRegistrationService.registerOrLogin(GOOGLE, PROVIDER_USER_ID, EMAIL, "처음닉네임");

            final LoginResult result = userRegistrationService.registerOrLogin(
                    GOOGLE, PROVIDER_USER_ID, EMAIL, "다른닉네임");

            assertThat(result.isNewUser()).isFalse();
        }

        @Test
        void 다른_provider이면_별도_User로_가입된다() {
            final User googleUser = userRegistrationService.registerOrLogin(
                    GOOGLE, PROVIDER_USER_ID, EMAIL, "구글유저").user();
            final User kakaoUser = userRegistrationService.registerOrLogin(
                    OAuthProvider.KAKAO, PROVIDER_USER_ID, EMAIL, "카카오유저").user();

            assertThat(kakaoUser.getId()).isNotEqualTo(googleUser.getId());
        }
    }
}
