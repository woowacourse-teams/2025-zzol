package coffeeshout.profanity.domain.audit;

public enum NicknameAuditStatus {
    UNAUDITED,
    FLAGGED,
    PENDING,
    CLEAN,
    ALLOWED,
    BLOCKED,
    /** 검열 호출이 상한만큼 되풀이 실패한 행. UNAUDITED 스캔에서 빠져 회차를 더는 붙잡지 않는다. */
    DEAD_LETTER
}
