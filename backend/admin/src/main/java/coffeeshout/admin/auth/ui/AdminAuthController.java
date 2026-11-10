package coffeeshout.admin.auth.ui;

import coffeeshout.admin.account.domain.AdminAccountErrorCode;
import coffeeshout.admin.auth.AdminAuthProperties;
import coffeeshout.admin.auth.application.AdminAuthService;
import coffeeshout.admin.auth.application.AdminTokens;
import coffeeshout.admin.auth.domain.AdminPrincipal;
import coffeeshout.admin.auth.ui.request.AdminLoginRequest;
import coffeeshout.admin.auth.ui.response.AdminMeResponse;
import coffeeshout.admin.auth.ui.response.AdminTokenResponse;
import coffeeshout.global.exception.custom.BusinessException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/api/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;
    private final AdminRefreshCookie adminRefreshCookie;
    private final AdminAuthProperties adminAuthProperties;

    /**
     * 구글 ID 토큰을 관리자 액세스 토큰으로 교환한다. refresh 토큰은 쿠키로 함께 내려준다.
     */
    @PostMapping("/login")
    public AdminTokenResponse login(@Valid @RequestBody AdminLoginRequest request, HttpServletResponse response) {
        return issue(adminAuthService.login(request.idToken()), response);
    }

    /**
     * refresh 쿠키로 새 액세스 토큰을 받는다. refresh 쿠키도 새 것으로 바뀐다.
     */
    @PostMapping("/refresh")
    public AdminTokenResponse refresh(
            @CookieValue(name = AdminRefreshCookie.NAME, required = false) String refreshToken,
            @RequestHeader(name = HttpHeaders.ORIGIN, required = false) String origin,
            HttpServletResponse response) {
        requireAdminOrigin(origin);
        return issue(adminAuthService.refresh(refreshToken), response);
    }

    /**
     * 서버에서 refresh 를 폐기하고 쿠키를 지운다. 액세스 토큰이 만료된 뒤에도 불러야 하므로 인증 없이 열려 있다.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = AdminRefreshCookie.NAME, required = false) String refreshToken,
            @RequestHeader(name = HttpHeaders.ORIGIN, required = false) String origin,
            HttpServletResponse response) {
        requireAdminOrigin(origin);
        adminAuthService.logout(refreshToken);
        adminRefreshCookie.clear(response);
        return ResponseEntity.noContent().build();
    }

    /**
     * 현재 토큰의 주체를 돌려준다. SPA 가 새로고침 후 세션 유효성을 확인할 때 쓴다.
     */
    @GetMapping("/me")
    public AdminMeResponse me(@AuthenticationPrincipal AdminPrincipal principal) {
        return new AdminMeResponse(principal.email().value());
    }

    private AdminTokenResponse issue(AdminTokens tokens, HttpServletResponse response) {
        adminRefreshCookie.set(response, tokens.refreshToken());
        return new AdminTokenResponse(tokens.accessToken());
    }

    /**
     * 쿠키로 인증하는 두 경로의 CSRF 방어다. 회원 프론트(www.zzol.site)는 같은 사이트라
     * SameSite 가 막지 못하고, CORS 허용 목록에도 있어 응답까지 읽을 수 있다. 그쪽에 XSS 가
     * 하나 생기면 관리자 브라우저에서 새 액세스 토큰을 받아 갈 수 있으므로 오리진을 직접 본다.
     *
     * <p>Origin 이 없으면 막는다. 브라우저는 POST 에 Origin 을 항상 붙인다.
     */
    private void requireAdminOrigin(String origin) {
        if (!adminAuthProperties.isWebOrigin(origin)) {
            throw new BusinessException(AdminAccountErrorCode.ADMIN_ORIGIN_NOT_ALLOWED, "허용되지 않은 오리진입니다: " + origin);
        }
    }
}
