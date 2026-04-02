package coffeeshout.numberpoker.domain;

import coffeeshout.minigame.domain.MiniGameScore;

/**
 * 넘버포커는 실시간으로 확률을 변동하므로 최종 점수는 사용되지 않는다.
 */
public class NumberPokerScore extends MiniGameScore {

    @Override
    public long getValue() {
        return 0;
    }
}
