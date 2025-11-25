package coffeeshout.racinggame.domain;

import coffeeshout.minigame.domain.MiniGameScore;

public class RacingGameScore extends MiniGameScore {

    private final long finishMillis;

    public RacingGameScore(long finishMillis) {
        this.finishMillis = finishMillis;
    }

    @Override
    public long getValue() {
        return finishMillis;
    }
}
