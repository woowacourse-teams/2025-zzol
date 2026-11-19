package coffeeshout.profanity.domain.audit;

public enum NicknameAuditStatus {
    UNAUDITED,
    FLAGGED,
    PENDING,
    CLEAN,
    ALLOWED,
    BLOCKED,
    /** 검열 호출이 상한만큼 되풀이 실패한 행. UNAUDITED 스캔에서 빠져 회차를 더는 붙잡지 않는다. */
    DEAD_LETTER;

    /** 랭킹처럼 불특정 다수에게 보이는 화면에 닉네임 원문을 내보내도 되는 판정이다(#1906). */
    public boolean isRevealable() {
        return this == CLEAN || this == ALLOWED;
    }
}
