package coffeeshout.blockstacking.domain;

import coffeeshout.minigame.domain.MiniGameScore;

public class BlockStackingScore extends MiniGameScore {

    private final int floor;

    public BlockStackingScore(int floor) {
        this.floor = floor;
    }

    @Override
    public long getValue() {
        return floor;
    }
}
