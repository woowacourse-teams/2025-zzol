package coffeeshout.global.ipblock;

import static coffeeshout.support.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.global.exception.GlobalErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Ip")
class IpTest {

    @Nested
    class 생성 {

        @ParameterizedTest
        @ValueSource(strings = {"0.0.0.0", "1.2.3.4", "127.0.0.1", "192.168.0.1", "255.255.255.255"})
        void 유효한_IPv4로_생성할_수_있다(String value) {
            assertThat(new Ip(value).value()).isEqualTo(value);
        }

        @ParameterizedTest
        @ValueSource(strings = {"::1", "2001:db8::1", "fe80::a00:27ff:fe4e:66a1", "0:0:0:0:0:0:0:1"})
        void 유효한_IPv6로_생성할_수_있다(String value) {
            assertThat(new Ip(value).value()).isEqualTo(value);
        }

        @ParameterizedTest
        @ValueSource(strings = {"256.1.1.1", "1.2.3", "1.2.3.4.5", "not-an-ip", "gggg::1", "1:2:3:4:5:6:7:8:9"})
        void 유효하지_않은_형식이면_예외를_던진다(String value) {
            assertCoffeeShoutException(
                    () -> new Ip(value),
                    GlobalErrorCode.VALIDATION_ERROR
            );
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        void null_또는_공백이면_예외를_던진다(String value) {
            assertCoffeeShoutException(
                    () -> new Ip(value),
                    GlobalErrorCode.VALIDATION_ERROR
            );
        }

        @ParameterizedTest
        @ValueSource(strings = {"1.2.3.4\nINJECTED", "1.2.3.4\r\n[WARN] fake log"})
        void 개행문자가_포함되면_예외를_던진다(String value) {
            assertCoffeeShoutException(
                    () -> new Ip(value),
                    GlobalErrorCode.VALIDATION_ERROR
            );
        }
    }

    @Nested
    class tryFrom {

        @Test
        void 유효한_값이면_Ip를_담은_Optional을_반환한다() {
            assertThat(Ip.tryFrom("1.2.3.4")).contains(new Ip("1.2.3.4"));
        }

        @Test
        void 유효하지_않은_값이면_빈_Optional을_반환한다() {
            assertThat(Ip.tryFrom("not-an-ip")).isEmpty();
        }
    }

    @Nested
    class toString_표현 {

        @Test
        void 로그_출력용으로_value만_반환한다() {
            assertThat(new Ip("1.2.3.4")).hasToString("1.2.3.4");
        }
    }
}
