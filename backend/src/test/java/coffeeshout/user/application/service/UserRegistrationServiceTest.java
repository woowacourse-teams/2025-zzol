package coffeeshout.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.global.ServiceTest;
import coffeeshout.user.domain.OAuthAccount;
import coffeeshout.user.domain.OAuthProvider;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.UserNickname;
import coffeeshout.user.domain.repository.UserRepository;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserRegistrationServiceTest extends ServiceTest {

    @Autowired
    UserRegistrationService userRegistrationService;

    @Autowired
    UserRepository userRepository;

    private static final OAuthProvider GOOGLE = OAuthProvider.GOOGLE;
    private static final String PROVIDER_USER_ID = "google-uid-42";
    private static final String EMAIL = "test@example.com";

    @Nested
    class 신규_가입 {

        @Test
        void 새_User가_생성되고_UserCode가_부여된다() {
            final User user = userRegistrationService.registerOrLogin(
                    GOOGLE, PROVIDER_USER_ID, EMAIL, "용감한호랑이");

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(user.getId()).isNotNull();
                softly.assertThat(user.getUserCode().value()).hasSize(5);
                softly.assertThat(user.getNickname().value()).isEqualTo("용감한호랑이");
            });
        }

        @Test
        void 닉네임이_null이면_자동_닉네임으로_가입된다() {
            final User user = userRegistrationService.registerOrLogin(
                    GOOGLE, PROVIDER_USER_ID, EMAIL, null);

            assertThat(user.getNickname().value())
                    .isNotBlank()
                    .hasSizeLessThanOrEqualTo(UserNickname.MAX_LENGTH);
        }

        @Test
        void 닉네임이_빈_문자열이면_자동_닉네임으로_가입된다() {
            final User user = userRegistrationService.registerOrLogin(
                    GOOGLE, PROVIDER_USER_ID, EMAIL, "");

            assertThat(user.getNickname().value())
                    .isNotBlank()
                    .hasSizeLessThanOrEqualTo(UserNickname.MAX_LENGTH);
        }

        @Test
        void 비속어_닉네임이면_자동_닉네임으로_대체된다() {
            final User user = userRegistrationService.registerOrLogin(
                    GOOGLE, PROVIDER_USER_ID, EMAIL, "씨발");

            assertThat(user.getNickname().value()).isNotEqualTo("씨발");
        }

        @Test
        void 닉네임이_최대_길이를_초과하면_잘라서_사용한다() {
            final User user = userRegistrationService.registerOrLogin(
                    GOOGLE, PROVIDER_USER_ID, EMAIL, "용감한호랑이열한글자초과");

            assertThat(user.getNickname().value().length())
                    .isLessThanOrEqualTo(UserNickname.MAX_LENGTH);
        }
    }

    @Nested
    class 기존_회원_로그인 {

        @Test
        void 동일_provider와_providerUserId이면_기존_User를_반환한다() {
            final User first = userRegistrationService.registerOrLogin(
                    GOOGLE, PROVIDER_USER_ID, EMAIL, "처음닉네임");

            final User second = userRegistrationService.registerOrLogin(
                    GOOGLE, PROVIDER_USER_ID, EMAIL, "다른닉네임");

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(second.getId()).isEqualTo(first.getId());
                softly.assertThat(second.getNickname().value()).isEqualTo("처음닉네임");
            });
        }

        @Test
        void 다른_provider이면_별도_User로_가입된다() {
            final User googleUser = userRegistrationService.registerOrLogin(
                    GOOGLE, PROVIDER_USER_ID, EMAIL, "구글유저");
            final User kakaoUser = userRegistrationService.registerOrLogin(
                    OAuthProvider.KAKAO, PROVIDER_USER_ID, EMAIL, "카카오유저");

            assertThat(kakaoUser.getId()).isNotEqualTo(googleUser.getId());
        }
    }
}
