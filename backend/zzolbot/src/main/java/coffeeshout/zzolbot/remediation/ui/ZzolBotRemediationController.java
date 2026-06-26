package coffeeshout.zzolbot.remediation.ui;

import coffeeshout.zzolbot.remediation.application.RemediationDecision;
import coffeeshout.zzolbot.remediation.application.RemediationTriggerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 자동 수정 트리거 어드민 API. 운영자가 모니터링 알림에서 "수정 시도"를 누르면 이 엔드포인트가 받는다.
 * 경로를 monitor 아래 둬서 기존 admin 보안·CSRF 설정이 그대로 적용된다(읽기 전용 monitor 조회와 한 화면).
 */
@RestController
@RequestMapping("/admin/zzolbot/monitor")
@RequiredArgsConstructor
public class ZzolBotRemediationController {

    private final RemediationTriggerService triggerService;

    @PostMapping("/alerts/{id}/remediate")
    public RemediationResponse remediate(@PathVariable Long id) {
        final RemediationDecision decision = triggerService.requestFix(id);
        return new RemediationResponse(decision.outcome().name(), decision.attemptId(), decision.message());
    }

    record RemediationResponse(String outcome, Long attemptId, String message) {}
}
