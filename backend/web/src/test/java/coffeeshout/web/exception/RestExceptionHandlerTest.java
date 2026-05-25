package coffeeshout.web.exception;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.global.exception.ErrorCode;
import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.global.ipblock.IpBlockAttributes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@DisplayName("RestExceptionHandler")
class RestExceptionHandlerTest {

    private RestExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RestExceptionHandler();
    }

    enum TestErrorCode implements ErrorCode {
        NOT_FOUND("TEST_NOT_FOUND", "Not found", 404),
        BAD_REQUEST("TEST_BAD_REQUEST", "Bad request", 400);

        private final String code;
        private final String message;
        private final int statusCode;

        TestErrorCode(String code, String message, int statusCode) {
            this.code = code;
            this.message = message;
            this.statusCode = statusCode;
        }

        @Override public String getCode() { return code; }
        @Override public String getMessage() { return message; }
        @Override public int getStatusCode() { return statusCode; }
    }

    @Nested
    class BusinessException_404_처리 {

        @Test
        void 비즈니스_예외가_404_상태면_IP_차단_스킵_속성을_설정한다() {
            final BusinessException exception = new BusinessException(TestErrorCode.NOT_FOUND, "Not found");
            final MockHttpServletRequest request = new MockHttpServletRequest();

            handler.handleBusinessException(exception, request);

            assertThat(request.getAttribute(IpBlockAttributes.BUSINESS_NOT_FOUND)).isEqualTo(true);
        }

        @Test
        void 비즈니스_예외가_404_외_상태면_IP_차단_스킵_속성을_설정하지_않는다() {
            final BusinessException exception = new BusinessException(TestErrorCode.BAD_REQUEST, "Bad request");
            final MockHttpServletRequest request = new MockHttpServletRequest();

            handler.handleBusinessException(exception, request);

            assertThat(request.getAttribute(IpBlockAttributes.BUSINESS_NOT_FOUND)).isNull();
        }
    }

    @Nested
    class NoResourceFoundException_처리 {

        @Test
        void 미등록_경로_접근_시_IP_차단_스킵_속성을_설정하지_않는다() {
            final NoResourceFoundException exception =
                    new NoResourceFoundException(HttpMethod.GET, "/random-probe");
            final MockHttpServletRequest request = new MockHttpServletRequest();

            handler.handleNoResourceFoundException(exception, request);

            assertThat(request.getAttribute(IpBlockAttributes.BUSINESS_NOT_FOUND)).isNull();
        }
    }
}
