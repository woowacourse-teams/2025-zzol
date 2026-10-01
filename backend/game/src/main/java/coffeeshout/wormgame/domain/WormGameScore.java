package coffeeshout.wormgame.domain;

import coffeeshout.minigame.domain.MiniGameScore;

public class WormGameScore extends MiniGameScore {

    private final long survivalMillis;

    public WormGameScore(long survivalMillis) {
        this.survivalMillis = survivalMillis;
    }

    @Override
    public long getValue() {
        return survivalMillis;
    }
}
