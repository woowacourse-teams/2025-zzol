package coffeeshout.laddergame.domain;

import coffeeshout.minigame.domain.MiniGameScore;

public class LadderGameScore extends MiniGameScore {

    private final int rank;

    public LadderGameScore(int rank) {
        this.rank = rank;
    }

    @Override
    public long getValue() {
        return rank;
    }
}
