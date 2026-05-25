package coffeeshout.profanity.domain.audit;

public record NicknameAuditResult(
        String nickname,
        NicknameAuditStatus status,
        AiConfidence confidence,
        String reason
) {

    public static NicknameAuditResult of(String nickname, boolean flagged, double confidence, String reason, double flaggedThreshold) {
        final AiConfidence aiConfidence = AiConfidence.of(confidence);

        if (!flagged) {
            return new NicknameAuditResult(nickname, NicknameAuditStatus.CLEAN, aiConfidence, reason);
        }
        if (confidence >= flaggedThreshold) {
            return new NicknameAuditResult(nickname, NicknameAuditStatus.FLAGGED, aiConfidence, reason);
        }
        return new NicknameAuditResult(nickname, NicknameAuditStatus.PENDING, aiConfidence, reason);
    }
}
