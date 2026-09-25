package coffeeshout.admin.quality.domain;

/**
 * 닉네임 검열 품질. AI 판정을 관리자가 얼마나 뒤집었는지를 잰다.
 *
 * <p>이 값이 검열 모델을 손봐야 하는 시점을 알려주는 유일한 신호다. 대기 건수는
 * 일이 얼마나 밀렸는지만 말해주지 모델이 잘하고 있는지는 말해주지 않는다.
 *
 * <p>{@code player_name_audit} 행의 상태와 표본 표시({@code review_sample})로 계산된다. AI가 CLEAN으로
 * 통과시킨 행은 다시 검열받지 않아 사람이 볼 길이 없으므로, 미탐은 회차마다 뽑은 표본에서만 센다.
 *
 * @param total         관리자가 결정한 행(ALLOWED·BLOCKED) 수
 * @param falsePositive AI 가 걸렀는데 관리자가 허용했다. 표본이 아닌 ALLOWED다. 멀쩡한 닉네임을 막고 있었다는 뜻이다.
 * @param falseNegative AI 가 통과시켰는데 관리자가 차단했다. 표본인 BLOCKED다. 걸러야 할 것을 통과시켰다는 뜻이다.
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
