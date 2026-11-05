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
 * @param sampleReviewed 관리자가 정상·미탐을 정한 표본 수. 미탐률 추정의 분모다.
 */
public record NicknameAuditQuality(long total, long falsePositive, long falseNegative, long sampleReviewed) {

    /** 95% 신뢰구간의 z 값. */
    private static final double Z = 1.96;

    public static NicknameAuditQuality empty() {
        return new NicknameAuditQuality(0, 0, 0, 0);
    }

    /**
     * CLEAN 판정 전체의 미탐률이 95% 확률로 넘지 않는 상한. 0.0 ~ 1.0. 검토한 표본이 없으면 null.
     *
     * <p>미탐이 있으면 Wilson 점수 구간의 위 끝을 쓴다. 표본이 적거나 비율이 0·1에 가까울 때도 구간이 0~1 밖으로
     * 나가지 않는다. 미탐이 0건이면 3의 법칙({@code 3 / n})을 쓴다. 표본 100건에 미탐 0건이면 화면이
     * "미탐률 0%"가 아니라 "3% 이하"라고 말해야 맞고, 이 값은 손으로 검산할 수 있다.
     */
    public Double missUpperBound() {
        if (sampleReviewed == 0) {
            return null;
        }
        final double n = sampleReviewed;
        if (falseNegative == 0) {
            return Math.min(1.0, 3 / n);
        }
        final double p = falseNegative / n;
        final double zz = Z * Z;
        final double center = p + zz / (2 * n);
        final double margin = Z * Math.sqrt(p * (1 - p) / n + zz / (4 * n * n));
        return Math.min(1.0, (center + margin) / (1 + zz / n));
    }

    /** 관리자가 AI 판정을 뒤집은 비율. 0.0 ~ 1.0. 판정이 없으면 0. */
    public double overrideRate() {
        return total == 0 ? 0 : (double) (falsePositive + falseNegative) / total;
    }

    public long agreed() {
        return total - falsePositive - falseNegative;
    }
}
