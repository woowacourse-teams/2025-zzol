package coffeeshout.fixture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.global.exception.ErrorCode;
import coffeeshout.global.exception.custom.CoffeeShoutException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

public class ExceptionAssertions {

    private ExceptionAssertions() {
    }

    public static void assertCoffeeShoutException(ThrowingCallable callable, ErrorCode expectedCode) {
        assertThatThrownBy(callable)
                .isInstanceOf(CoffeeShoutException.class)
                .satisfies(e -> assertThat(((CoffeeShoutException) e).getErrorCode())
                        .isEqualTo(expectedCode));
    }
}
