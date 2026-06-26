package coffeeshout.zzolbot.remediation.ui;

import coffeeshout.zzolbot.remediation.application.RemediationCallbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GitHub Actions 워커가 수정 시도 결과를 보고하는 내부 콜백 수신기.
 *
 * <p><b>내부 전용.</b> {@code /internal/**}이라 기존 {@code InternalWebhookSecurityConfig}의 베어러 토큰
 * 필터가 그대로 막는다(Alertmanager 웹훅과 동일 게이트). nginx 내부 리스너로만 도달한다.
 */
@RestController
@RequestMapping("/internal/zzolbot/remediation")
@RequiredArgsConstructor
public class RemediationCallbackController {

    private final RemediationCallbackService callbackService;

    @PostMapping("/attempts/{id}")
    public ResponseEntity<Void> report(@PathVariable Long id, @RequestBody RemediationCallbackRequest request) {
        callbackService.apply(id, request.status(), request.prUrl(), request.prNumber(),
                request.branchName(), request.detail());
        return ResponseEntity.ok().build();
    }
}
