package coffeeshout.zzolbot.remediation.application;

import coffeeshout.zzolbot.remediation.domain.DefectType;

/**
 * 수정 시도 트리거의 결과. 게이트에서 막힌 사유를 운영자에게 즉시 돌려주기 위한 값이다.
 * {@link Outcome#DISPATCHED}일 때만 시도가 영속되며 {@code attemptId}가 채워진다.
 */
public record RemediationDecision(Outcome outcome, Long attemptId, DefectType defectType, String message) {

    public enum Outcome {
        DISPATCHED,
        DISABLED,
        RUN_NOT_FOUND,
        NOT_A_CODE_DEFECT,
        COOLDOWN,
        BUDGET_EXHAUSTED,
        DISPATCH_FAILED
    }

    public static RemediationDecision dispatched(Long attemptId, DefectType defectType) {
        return new RemediationDecision(Outcome.DISPATCHED, attemptId, defectType, "자동 수정 워크플로우를 디스패치했습니다.");
    }

    public static RemediationDecision of(Outcome outcome, String message) {
        return new RemediationDecision(outcome, null, null, message);
    }
}
