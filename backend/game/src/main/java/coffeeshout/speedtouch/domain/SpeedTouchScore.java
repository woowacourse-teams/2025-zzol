package coffeeshout.speedtouch.domain;

import coffeeshout.minigame.domain.MiniGameScore;

/**
 * 단일 long 값으로 완주자/DNF를 모두 표현하는 점수 체계.
 *
 * - 완주자: finishMillis (0 ~ 제한시간). 값이 작을수록 빠르게 완주 → 높은 순위.
 * - DNF: DNF_BASE - progress. progress가 높을수록 값이 작아져 → DNF 내에서 높은 순위.
 * - 완주자의 최댓값(제한시간 ms)은 DNF_BASE보다 항상 작으므로, 완주자는 무조건 DNF보다 앞선다.
 *
 * MiniGameResult.fromAscending()으로 오름차순 정렬하면 원하는 랭킹이 나온다.
 */
public class SpeedTouchScore extends MiniGameScore {

    private static final long DNF_BASE = 1_000_000_000L;

    private final long value;

    private SpeedTouchScore(long value) {
        this.value = value;
    }

    public static SpeedTouchScore ofFinished(long finishMillis) {
        return new SpeedTouchScore(finishMillis);
    }

    public static SpeedTouchScore ofDnf(int progress) {
        return new SpeedTouchScore(DNF_BASE - progress);
    }

    @Override
    public long getValue() {
        return value;
    }
}
