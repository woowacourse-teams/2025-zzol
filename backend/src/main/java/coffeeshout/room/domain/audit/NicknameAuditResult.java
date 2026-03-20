package coffeeshout.room.domain.audit;

public record NicknameAuditResult(
        String nickname,
        NicknameAuditStatus status,
        double confidence,
        String reason
) {

    public static NicknameAuditResult of(String nickname, boolean flagged, double confidence, String reason, double flaggedThreshold) {
        if (!flagged) {
            return new NicknameAuditResult(nickname, NicknameAuditStatus.CLEAN, confidence, reason);
        }
        if (confidence >= flaggedThreshold) {
            return new NicknameAuditResult(nickname, NicknameAuditStatus.FLAGGED, confidence, reason);
        }
        return new NicknameAuditResult(nickname, NicknameAuditStatus.PENDING, confidence, reason);
    }
}
