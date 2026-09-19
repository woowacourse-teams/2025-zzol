package coffeeshout.admin.auth.ui;

import coffeeshout.admin.auth.application.AdminAuthService;
import coffeeshout.admin.auth.domain.AdminPrincipal;
import coffeeshout.admin.auth.ui.request.AdminLoginRequest;
import coffeeshout.admin.auth.ui.response.AdminMeResponse;
import coffeeshout.admin.auth.ui.response.AdminTokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/api/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    /**
     * 구글 ID 토큰을 관리자 액세스 토큰으로 교환한다. 이 경로만 인증 없이 열려 있다.
     */
    @PostMapping("/login")
    public AdminTokenResponse login(@Valid @RequestBody AdminLoginRequest request) {
        return new AdminTokenResponse(adminAuthService.login(request.idToken()));
    }

    /**
     * 현재 토큰의 주체를 돌려준다. SPA 가 새로고침 후 세션 유효성을 확인할 때 쓴다.
     */
    @GetMapping("/me")
    public AdminMeResponse me(@AuthenticationPrincipal AdminPrincipal principal) {
        return new AdminMeResponse(principal.email().value());
    }
}
