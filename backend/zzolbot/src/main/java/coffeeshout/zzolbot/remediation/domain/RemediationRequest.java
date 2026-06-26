package coffeeshout.zzolbot.remediation.domain;

import java.util.List;

/**
 * GitHub Actions 워커로 넘기는 수정 요청(repository_dispatch의 client_payload가 될 도메인 뷰).
 * 워커는 stackTrace로 결함 위치를 특정하고, defectType으로 수정 전략을 고르며, attemptId로 결과를 콜백한다.
 */
public record RemediationRequest(
        Long attemptId,
        Long monitorRunId,
        String fingerprint,
        String alertname,
        String severity,
        DefectType defectType,
        String rootCauseHypothesis,
        List<String> suggestedActions,
        String stackTrace) {

    public RemediationRequest {
        suggestedActions = suggestedActions == null ? List.of() : List.copyOf(suggestedActions);
    }
}
