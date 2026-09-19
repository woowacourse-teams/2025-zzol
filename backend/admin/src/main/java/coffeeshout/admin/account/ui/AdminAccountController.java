package coffeeshout.admin.account.ui;

import coffeeshout.admin.account.application.AdminAccountService;
import coffeeshout.admin.account.domain.AdminEmail;
import coffeeshout.admin.account.ui.request.AddAdminAccountRequest;
import coffeeshout.admin.account.ui.response.AdminAccountResponse;
import coffeeshout.admin.auth.domain.AdminPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 계정 관리. 이미 관리자인 사람만 다른 관리자를 추가하거나 지울 수 있다.
 *
 * <p>승인 대기 단계를 두지 않는다. 운영자가 소수인 지금은 신청과 승인을 나눠 봐야
 * 같은 사람이 양쪽을 누르게 될 뿐이다. 대신 누가 누구를 추가했는지는 남긴다.
 *
 * <p>쓰기 요청은 {@code AdminAuditAspect}가 자동으로 감사 로그에 남긴다.
 */
@RestController
@RequestMapping("/admin/api/accounts")
@RequiredArgsConstructor
public class AdminAccountController {

    private final AdminAccountService adminAccountService;

    @GetMapping
    public List<AdminAccountResponse> list() {
        return adminAccountService.list().stream()
                .map(AdminAccountResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminAccountResponse add(
            @Valid @RequestBody AddAdminAccountRequest request, @AuthenticationPrincipal AdminPrincipal principal) {
        // 관리자가 직접 입력한 값이라 형식이 틀리면 틀렸다고 알려준다.
        // 로그인 판정과 달리 여기서는 숨길 이유가 없다.
        return AdminAccountResponse.from(adminAccountService.add(AdminEmail.of(request.email()), principal.email()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long id, @AuthenticationPrincipal AdminPrincipal principal) {
        adminAccountService.remove(id, principal.email());
    }
}
