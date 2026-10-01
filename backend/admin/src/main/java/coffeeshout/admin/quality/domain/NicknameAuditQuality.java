package coffeeshout.admin.quality.domain;

/**
 * 닉네임 검열 품질. AI 판정을 관리자가 얼마나 뒤집었는지를 잰다.
 *
 * <p>이 값이 검열 모델을 손봐야 하는 시점을 알려주는 유일한 신호다. 대기 건수는
 * 일이 얼마나 밀렸는지만 말해주지 모델이 잘하고 있는지는 말해주지 않는다.
 *
 * <p>{@code player_name_feedback} 한 테이블로 계산된다. 그 테이블이 AI 판정({@code aiFlagged})과
 * 관리자 결정({@code operatorDecision})을 나란히 들고 있어 별도 계측이 필요 없다.
 *
 * @param falsePositive AI 가 걸렀는데 관리자가 허용했다. 멀쩡한 닉네임을 막고 있었다는 뜻이다.
 * @param falseNegative AI 가 놓쳤는데 관리자가 차단했다. 걸러야 할 것을 통과시켰다는 뜻이다.
 */
public record NicknameAuditQuality(long total, long falsePositive, long falseNegative) {

    public static NicknameAuditQuality empty() {
        return new NicknameAuditQuality(0, 0, 0);
    }

    /** 관리자가 AI 판정을 뒤집은 비율. 0.0 ~ 1.0. 판정이 없으면 0. */
    public double overrideRate() {
        return total == 0 ? 0 : (double) (falsePositive + falseNegative) / total;
    }

    public long agreed() {
        return total - falsePositive - falseNegative;
    }
}
