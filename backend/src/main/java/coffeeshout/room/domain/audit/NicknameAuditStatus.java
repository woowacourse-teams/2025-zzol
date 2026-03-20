package coffeeshout.room.domain.audit;

public enum NicknameAuditStatus {
    UNAUDITED,
    FLAGGED,
    PENDING,
    CLEAN,
    ALLOWED,
    BLOCKED
}
