package coffeeshout.blockstacking.domain;

import coffeeshout.minigame.domain.MiniGameScore;
import java.util.Objects;

public class BlockStackingScore extends MiniGameScore {

    private final int floor;

    public BlockStackingScore(int floor) {
        this.floor = floor;
    }

    @Override
    public long getValue() {
        return floor;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        final BlockStackingScore that = (BlockStackingScore) o;
        return floor == that.floor;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), floor);
    }
}
