package coffeeshout.room.domain.audit;

public record PlayerNameAuditResult(
        String playerName,
        PlayerNameAuditStatus status,
        AiConfidence confidence,
        String reason
) {

    public static PlayerNameAuditResult of(String playerName, boolean flagged, double confidence, String reason, double flaggedThreshold) {
        final AiConfidence aiConfidence = AiConfidence.of(confidence);

        if (!flagged) {
            return new PlayerNameAuditResult(playerName, PlayerNameAuditStatus.CLEAN, aiConfidence, reason);
        }
        if (confidence >= flaggedThreshold) {
            return new PlayerNameAuditResult(playerName, PlayerNameAuditStatus.FLAGGED, aiConfidence, reason);
        }
        return new PlayerNameAuditResult(playerName, PlayerNameAuditStatus.PENDING, aiConfidence, reason);
    }
}
