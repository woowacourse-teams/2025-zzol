package coffeeshout.room.domain.audit;

public enum PlayerNameAuditStatus {
    UNAUDITED,
    FLAGGED,
    PENDING,
    CLEAN,
    ALLOWED,
    BLOCKED
}
