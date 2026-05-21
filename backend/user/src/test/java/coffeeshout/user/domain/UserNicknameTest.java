package coffeeshout.user.domain;

import static coffeeshout.fixture.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.user.domain.UserErrorCode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UserNicknameTest {

    @Nested
    class 유효한_닉네임 {

        @ParameterizedTest
        @ValueSource(strings = {"홍길동", "커피빵팀", "1234567890"})
        void 최대_길이_이하이고_공백이_아니면_생성된다(String value) {
            final UserNickname nickname = new UserNickname(value);

            assertThat(nickname.value()).isEqualTo(value);
        }
    }

    @Nested
    class 유효하지_않은_닉네임 {

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "  "})
        void 공백이면_예외가_발생한다(String value) {
            assertCoffeeShoutException(
                    () -> new UserNickname(value),
                    UserErrorCode.NICKNAME_BLANK
            );
        }

        @Test
        void null이면_예외가_발생한다() {
            assertCoffeeShoutException(
                    () -> new UserNickname(null),
                    UserErrorCode.NICKNAME_BLANK
            );
        }

        @Test
        void 최대_길이_초과이면_예외가_발생한다() {
            assertCoffeeShoutException(
                    () -> new UserNickname("12345678901"),
                    UserErrorCode.NICKNAME_TOO_LONG
            );
        }
    }
}
