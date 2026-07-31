package coffeeshout.settlement.domain;

import coffeeshout.global.exception.GlobalErrorCode;
import coffeeshout.global.exception.custom.SystemException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 시즌 누적 포인트로 판정하는 티어. 임계는 월 시즌 기준 대략적인 플레이 횟수를 가정했다
 * (게임당 30~100점 — SILVER는 몇 판, DIAMOND는 꾸준한 상위권).
 */
public enum SeasonTier {
    BRONZE(0),
    SILVER(300),
    GOLD(1000),
    DIAMOND(3000);

    private static final List<SeasonTier> DESCENDING = Arrays.stream(values())
            .sorted(Comparator.comparingLong(SeasonTier::getThreshold).reversed())
            .toList();

    private final long threshold;

    SeasonTier(long threshold) {
        this.threshold = threshold;
    }

    public static SeasonTier fromPoints(long totalPoints) {
        if (totalPoints < 0) {
            throw new SystemException(GlobalErrorCode.INTERNAL_SERVER_ERROR, "포인트는 0 이상이어야 합니다: " + totalPoints);
        }
        return DESCENDING.stream()
                .filter(tier -> totalPoints >= tier.threshold)
                .findFirst()
                .orElse(BRONZE);
    }

    public long getThreshold() {
        return threshold;
    }
}
