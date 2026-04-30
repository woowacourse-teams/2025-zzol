package coffeeshout.user.domain;

import static coffeeshout.global.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.user.exception.UserErrorCode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UserCodeTest {

    @Nested
    class 코드_생성 {

        @RepeatedTest(10)
        void 생성된_코드는_5자리이고_허용된_문자만_포함한다() {
            final UserCode code = UserCode.generate();

            assertThat(code.value()).hasSize(5);
            assertThat(code.value().chars())
                    .allMatch(c -> UserCode.CHARSET.indexOf(c) >= 0);
        }

        @RepeatedTest(5)
        void 반복_생성해도_동일한_코드가_아닐_수_있다() {
            final UserCode code1 = UserCode.generate();
            final UserCode code2 = UserCode.generate();

            assertThat(code1).isNotNull();
            assertThat(code2).isNotNull();
        }
    }

    @Nested
    class 코드_검증 {

        @ParameterizedTest
        @ValueSource(strings = {"ABCDF", "34678", "GHJKL"})
        void 유효한_5자리_코드는_생성된다(String value) {
            final UserCode code = new UserCode(value);

            assertThat(code.value()).isEqualTo(value);
        }

        @Test
        void null이면_예외가_발생한다() {
            assertCoffeeShoutException(
                    () -> new UserCode(null),
                    UserErrorCode.NICKNAME_INVALID
            );
        }

        @ParameterizedTest
        @ValueSource(strings = {"ABCD", "ABCDFG"})
        void 길이가_5가_아니면_예외가_발생한다(String value) {
            assertCoffeeShoutException(
                    () -> new UserCode(value),
                    UserErrorCode.NICKNAME_INVALID
            );
        }

        @ParameterizedTest
        @ValueSource(strings = {"ABCE1", "1ABCD", "abcdf"})
        void 허용되지_않는_문자가_포함되면_예외가_발생한다(String value) {
            assertCoffeeShoutException(
                    () -> new UserCode(value),
                    UserErrorCode.NICKNAME_INVALID
            );
        }
    }

    @Nested
    class 동등성 {

        @Test
        void 같은_값이면_동등하다() {
            final UserCode code1 = new UserCode("ABCDF");
            final UserCode code2 = new UserCode("ABCDF");

            assertThat(code1).isEqualTo(code2);
        }

        @Test
        void 다른_값이면_동등하지_않다() {
            final UserCode code1 = new UserCode("ABCDF");
            final UserCode code2 = new UserCode("GHJKL");

            assertThat(code1).isNotEqualTo(code2);
        }
    }
}
