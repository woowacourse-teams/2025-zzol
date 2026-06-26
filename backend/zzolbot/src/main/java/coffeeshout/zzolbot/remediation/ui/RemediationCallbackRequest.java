package coffeeshout.zzolbot.remediation.ui;

import coffeeshout.zzolbot.remediation.domain.RemediationStatus;

/**
 * GitHub Actions 워커가 작업 결과를 보고하는 내부 콜백 본문.
 * {@code status}는 PR_OPENED·NO_FIX·FAILED 중 하나이며, PR_OPENED일 때 prUrl·prNumber·branchName이 채워진다.
 */
public record RemediationCallbackRequest(
        RemediationStatus status,
        String prUrl,
        Integer prNumber,
        String branchName,
        String detail) {
}
