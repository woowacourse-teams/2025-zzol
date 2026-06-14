package coffeeshout.profanity.domain.audit;

public enum NicknameAuditStatus {
    UNAUDITED,
    FLAGGED,
    PENDING,
    CLEAN,
    ALLOWED,
    BLOCKED
}
