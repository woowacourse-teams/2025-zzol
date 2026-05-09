package coffeeshout.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.global.filter.IpBlockAttributes;
import coffeeshout.user.exception.UserErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

@DisplayName("RestExceptionHandler")
class RestExceptionHandlerTest {

    private RestExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RestExceptionHandler();
    }

    @Nested
    class BusinessException_404_처리 {

        @Test
        void 비즈니스_예외가_404_상태면_IP_차단_스킵_속성을_설정한다() {
            final BusinessException exception = new BusinessException(UserErrorCode.USER_NOT_FOUND, "존재하지 않는 회원");
            final MockHttpServletRequest request = new MockHttpServletRequest();

            handler.handleBusinessException(exception, request);

            assertThat(request.getAttribute(IpBlockAttributes.BUSINESS_NOT_FOUND)).isEqualTo(true);
        }

        @Test
        void 비즈니스_예외가_404_외_상태면_IP_차단_스킵_속성을_설정하지_않는다() {
            final BusinessException exception = new BusinessException(UserErrorCode.NICKNAME_BLANK, "닉네임 오류");
            final MockHttpServletRequest request = new MockHttpServletRequest();

            handler.handleBusinessException(exception, request);

            assertThat(request.getAttribute(IpBlockAttributes.BUSINESS_NOT_FOUND)).isNull();
        }
    }

    @Nested
    class NoResourceFoundException_처리 {

        @Test
        void 미등록_경로_접근_시_IP_차단_스킵_속성을_설정하지_않는다() {
            final org.springframework.web.servlet.resource.NoResourceFoundException exception =
                    new org.springframework.web.servlet.resource.NoResourceFoundException(
                            org.springframework.http.HttpMethod.GET, "/random-probe");
            final MockHttpServletRequest request = new MockHttpServletRequest();

            handler.handleNoResourceFoundException(exception, request);

            assertThat(request.getAttribute(IpBlockAttributes.BUSINESS_NOT_FOUND)).isNull();
        }
    }
}
