package coffeeshout.admin.support;

import coffeeshout.admin.ipblock.IpBlockAdminController;
import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.report.ui.ReportAdminController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Thymeleaf 어드민 컨트롤러 전용 예외 핸들러.
 * <p>
 * 전역 {@code RestExceptionHandler}({@code @RestControllerAdvice})는 ProblemDetail JSON을 반환하므로,
 * 브라우저 폼 제출인 어드민 화면에서는 JSON 본문이 그대로 노출된다.
 * 이 advice가 더 높은 우선순위로 가로채 flash 메시지와 함께 원래 페이지로 리다이렉트한다.
 * <p>
 * open redirect를 막기 위해 Referer의 path·query만 사용한다.
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice(assignableTypes = {IpBlockAdminController.class, ReportAdminController.class})
public class AdminViewExceptionHandler {

    private static final String FALLBACK_PATH = "/admin";

    @ExceptionHandler(BusinessException.class)
    public String handleBusinessException(
            BusinessException e,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        log.warn("어드민 요청 처리 실패: uri={} message={}", request.getRequestURI(), e.getMessage());
        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        return redirectToReferer(request);
    }

    @ExceptionHandler({ConstraintViolationException.class, HandlerMethodValidationException.class})
    public String handleValidationException(
            Exception e,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        log.warn("어드민 요청 파라미터 검증 실패: uri={} message={}", request.getRequestURI(), e.getMessage());
        redirectAttributes.addFlashAttribute("errorMessage", "요청 파라미터가 유효하지 않습니다.");
        return redirectToReferer(request);
    }

    private String redirectToReferer(HttpServletRequest request) {
        final String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return "redirect:" + FALLBACK_PATH;
        }
        try {
            final URI uri = URI.create(referer);
            final String path = uri.getPath() != null && !uri.getPath().isBlank() ? uri.getPath() : FALLBACK_PATH;
            final String query = uri.getQuery();
            return "redirect:" + (query != null ? path + "?" + query : path);
        } catch (IllegalArgumentException e) {
            return "redirect:" + FALLBACK_PATH;
        }
    }
}
