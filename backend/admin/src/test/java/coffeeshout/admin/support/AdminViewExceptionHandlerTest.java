package coffeeshout.admin.support;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.global.exception.GlobalErrorCode;
import coffeeshout.global.exception.custom.BusinessException;
import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

@DisplayName("AdminViewExceptionHandler")
class AdminViewExceptionHandlerTest {

    private AdminViewExceptionHandler handler;
    private MockHttpServletRequest request;
    private RedirectAttributesModelMap redirectAttributes;

    @BeforeEach
    void setUp() {
        handler = new AdminViewExceptionHandler();
        request = new MockHttpServletRequest();
        redirectAttributes = new RedirectAttributesModelMap();
    }

    @Nested
    class BusinessException_처리 {

        @Test
        void 예외_메시지를_flash에_담고_Referer_경로로_리다이렉트한다() {
            request.addHeader("Referer", "https://admin.example.com/admin/ip-blocks");
            final BusinessException e = new BusinessException(GlobalErrorCode.VALIDATION_ERROR, "유효하지 않은 IP 형식입니다.");

            final String view = handler.handleBusinessException(e, request, redirectAttributes);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(view).isEqualTo("redirect:/admin/ip-blocks");
                softly.assertThat(redirectAttributes.getFlashAttributes().get("errorMessage"))
                        .isEqualTo("유효하지 않은 IP 형식입니다.");
            });
        }

        @Test
        void Referer의_쿼리스트링을_보존한다() {
            request.addHeader("Referer", "https://admin.example.com/admin/reports?page=2&status=PENDING");
            final BusinessException e = new BusinessException(GlobalErrorCode.NOT_EXIST, "신고를 찾을 수 없습니다.");

            final String view = handler.handleBusinessException(e, request, redirectAttributes);

            assertThat(view).isEqualTo("redirect:/admin/reports?page=2&status=PENDING");
        }

        @Test
        void Referer가_없으면_어드민_홈으로_리다이렉트한다() {
            final BusinessException e = new BusinessException(GlobalErrorCode.VALIDATION_ERROR, "유효하지 않은 IP 형식입니다.");

            final String view = handler.handleBusinessException(e, request, redirectAttributes);

            assertThat(view).isEqualTo("redirect:/admin");
        }

        @Test
        void Referer가_비정상이면_어드민_홈으로_리다이렉트한다() {
            request.addHeader("Referer", "ht tp://broken referer");
            final BusinessException e = new BusinessException(GlobalErrorCode.VALIDATION_ERROR, "유효하지 않은 IP 형식입니다.");

            final String view = handler.handleBusinessException(e, request, redirectAttributes);

            assertThat(view).isEqualTo("redirect:/admin");
        }

        @Test
        void 다른_호스트의_Referer라도_경로만_사용해_open_redirect를_차단한다() {
            request.addHeader("Referer", "https://evil.example.com/phishing");
            final BusinessException e = new BusinessException(GlobalErrorCode.VALIDATION_ERROR, "유효하지 않은 IP 형식입니다.");

            final String view = handler.handleBusinessException(e, request, redirectAttributes);

            assertThat(view).isEqualTo("redirect:/phishing");
        }
    }

    @Nested
    class 검증_예외_처리 {

        @Test
        void 고정_메시지를_flash에_담고_Referer_경로로_리다이렉트한다() {
            request.addHeader("Referer", "https://admin.example.com/admin/reports?page=0");
            final ConstraintViolationException e = new ConstraintViolationException("page: 0 이상이어야 합니다", Set.of());

            final String view = handler.handleValidationException(e, request, redirectAttributes);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(view).isEqualTo("redirect:/admin/reports?page=0");
                softly.assertThat(redirectAttributes.getFlashAttributes().get("errorMessage"))
                        .isEqualTo("요청 파라미터가 유효하지 않습니다.");
            });
        }
    }
}
