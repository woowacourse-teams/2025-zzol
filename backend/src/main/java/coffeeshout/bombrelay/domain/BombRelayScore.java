package coffeeshout.bombrelay.domain;

import coffeeshout.minigame.domain.MiniGameScore;

/**
 * 탈락 순서 기반 점수 체계.
 *
 * - 생존자: SURVIVOR_SCORE (0). 공동 1등.
 * - 탈락자: 탈락 라운드 번호 (1, 2, 3). 먼저 탈락할수록 값이 작다.
 *
 * MiniGameResult.fromDescending()으로 내림차순 정렬하면 원하는 랭킹이 나온다.
 * (생존자 0 > 3라운드 탈락 3 > 2라운드 탈락 2 > 1라운드 탈락 1)
 *
 * → 아니다, fromAscending으로 오름차순 정렬해야 한다.
 *   생존자(0)가 1등, 1라운드 탈락(1)이 꼴등.
 */
public class BombRelayScore extends MiniGameScore {

    private static final long SURVIVOR_SCORE = 0L;

    private final long value;

    private BombRelayScore(long value) {
        this.value = value;
    }

    public static BombRelayScore ofSurvivor() {
        return new BombRelayScore(SURVIVOR_SCORE);
    }

    public static BombRelayScore ofEliminated(int eliminatedRound, int maxRounds) {
        // 1라운드 탈락 = maxRounds, 2라운드 탈락 = maxRounds-1, ...
        // 먼저 탈락할수록 높은 값 = 낮은 순위
        return new BombRelayScore(maxRounds - eliminatedRound + 1);
    }

    @Override
    public long getValue() {
        return value;
    }
}
