package coffeeshout.room.domain.audit;

public record PlayerNameAuditResult(
        String playerName,
        PlayerNameAuditStatus status,
        double confidence,
        String reason
) {

    public static PlayerNameAuditResult of(String playerName, boolean flagged, double confidence, String reason, double flaggedThreshold) {
        if (!flagged) {
            return new PlayerNameAuditResult(playerName, PlayerNameAuditStatus.CLEAN, confidence, reason);
        }
        if (confidence >= flaggedThreshold) {
            return new PlayerNameAuditResult(playerName, PlayerNameAuditStatus.FLAGGED, confidence, reason);
        }
        return new PlayerNameAuditResult(playerName, PlayerNameAuditStatus.PENDING, confidence, reason);
    }
}
