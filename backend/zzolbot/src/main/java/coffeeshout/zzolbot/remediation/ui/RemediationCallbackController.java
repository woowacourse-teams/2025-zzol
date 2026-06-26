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
 * GitHub Actions 워커가 수정 시도 결과를 보고하는 콜백 수신기.
 *
 * <p><b>공개 + 토큰 가드.</b> 워커가 GitHub 호스티드 러너(외부)에서 오므로 네트워크 격리가 불가능해
 * {@code /webhook/**} 공개 경로로 두고, {@link coffeeshout.zzolbot.remediation.config.RemediationCallbackSecurityConfig}의
 * 베어러 토큰 필터가 단일 게이트로 막는다. 이 경로는 수정 시도 상태/PR 링크만 갱신한다(코드 실행·데이터 노출 없음).
 */
@RestController
@RequestMapping("/webhook/zzolbot/remediation")
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
