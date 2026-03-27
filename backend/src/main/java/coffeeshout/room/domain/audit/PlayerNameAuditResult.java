package coffeeshout.room.domain.audit;

public record PlayerNameAuditResult(
        String playerName,
        PlayerNameAuditStatus status,
        double confidence,
        String reason
) {

    public static PlayerNameAuditResult of(String playerName, boolean flagged, double confidence, String reason, double flaggedThreshold) {
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("confidence는 0.0~1.0 사이여야 합니다.");
        }

        if (!flagged) {
            return new PlayerNameAuditResult(playerName, PlayerNameAuditStatus.CLEAN, confidence, reason);
        }
        if (confidence >= flaggedThreshold) {
            return new PlayerNameAuditResult(playerName, PlayerNameAuditStatus.FLAGGED, confidence, reason);
        }
        return new PlayerNameAuditResult(playerName, PlayerNameAuditStatus.PENDING, confidence, reason);
    }
}
