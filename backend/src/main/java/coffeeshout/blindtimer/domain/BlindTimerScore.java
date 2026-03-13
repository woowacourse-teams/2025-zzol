package coffeeshout.blindtimer.domain;

import coffeeshout.minigame.domain.MiniGameScore;
import java.time.Duration;

/**
 * 오차 기반 점수 체계.
 *
 * - 정상 STOP: |목표시간 - 멈춘시간| (밀리초). 오차가 작을수록 높은 순위.
 * - 타임아웃: TIMEOUT_PENALTY. 무조건 꼴등 그룹.
 * - 정상 STOP의 최대 오차는 약 20초(20000ms)이므로 TIMEOUT_PENALTY보다 항상 작다.
 *
 * MiniGameResult.fromAscending()으로 오름차순 정렬하면 원하는 랭킹이 나온다.
 */
public class BlindTimerScore extends MiniGameScore {

    private static final long TIMEOUT_PENALTY = 999_999_999L;

    private final long value;

    private BlindTimerScore(long value) {
        this.value = value;
    }

    public static BlindTimerScore ofNormal(Duration targetTime, Duration stoppedElapsed) {
        final Duration diff = targetTime.minus(stoppedElapsed);
        final long errorMillis = Math.abs(diff.toMillis());
        return new BlindTimerScore(errorMillis);
    }

    public static BlindTimerScore ofTimeout() {
        return new BlindTimerScore(TIMEOUT_PENALTY);
    }

    @Override
    public long getValue() {
        return value;
    }
}
