package coffeeshout.wormgame.domain;

/**
 * 빛꼬리 물리·판정 상수. 시간의 단일 진실은 틱 카운트다 — 이동·아레나 반지름·점수가 전부
 * 틱 번호에서 유도되며 벽시계는 쓰지 않는다(설계 문서 v0.3 「시간·판정」).
 */
public record WormGameRules(
        long tickMillis,
        double baseSpeed,
        double maxSpeedMultiplier,
        int speedRampTicks,
        double omegaBaseRadians,
        double omegaExponent,
        double arenaBaseRadius,
        int shrinkDelayTicks,
        int shrinkDurationTicks,
        double shrinkMinRatio,
        double lateShrinkPerTick,
        double minRadius,
        int invincibleTicks,
        double trailRadius,
        int wetPaintSkipSegments,
        int selfSkipSegments) {

    public static WormGameRules defaults() {
        return new WormGameRules(
                50L, 120.0, 1.6, 1200, Math.toRadians(200), 0.7, 220.0, 200, 1200, 0.30, 0.05, 40.0, 40, 6.0, 3, 5);
    }

    public double speedUnitsPerSecond(long tick) {
        final double ramp = Math.min(1.0, tick / (double) speedRampTicks);
        return baseSpeed * (1 + ramp * (maxSpeedMultiplier - 1));
    }

    public double speedPerTick(long tick) {
        return speedUnitsPerSecond(tick) * tickMillis / 1000.0;
    }

    /**
     * 회전 각속도 상한(라디안/틱). 속도 부분 비례 ω = ω₀·(v/v₀)^0.7 — 고정 상한이면 축소 후반에
     * 회전 지름이 아레나보다 커져 조작 불능 구간이 생긴다(교차 리뷰 검증). 지수가 난이도 튜닝 노브다.
     */
    public double omegaPerTick(long tick) {
        final double scale = Math.pow(speedUnitsPerSecond(tick) / baseSpeed, omegaExponent);
        return omegaBaseRadians * scale * tickMillis / 1000.0;
    }

    /**
     * 아레나 반지름. 면적 ∝ 인원수(R₀ = base·√(N/4)). 축소 완료 후에도 완만히 계속 줄여
     * 가장자리 소극 플레이의 정체 구간을 없앤다(교차 리뷰 결정).
     */
    public double arenaRadius(int playerCount, long tick) {
        final double initial = initialRadius(playerCount);
        if (tick < shrinkDelayTicks) {
            return initial;
        }
        final long shrinkTick = tick - shrinkDelayTicks;
        if (shrinkTick <= shrinkDurationTicks) {
            return initial * (1 - (1 - shrinkMinRatio) * shrinkTick / (double) shrinkDurationTicks);
        }
        final double late = initial * shrinkMinRatio - lateShrinkPerTick * (shrinkTick - shrinkDurationTicks);
        return Math.max(minRadius, late);
    }

    public double initialRadius(int playerCount) {
        return arenaBaseRadius * Math.sqrt(playerCount / 4.0);
    }
}
