package coffeeshout.minigame.domain;

import java.util.Objects;

public abstract class MiniGameScore implements Comparable<MiniGameScore> {

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MiniGameScore that = (MiniGameScore) o;
        return getValue() == that.getValue();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getValue());
    }

    @Override
    public int compareTo(final MiniGameScore o) {
        return Long.compare(this.getValue(), o.getValue());
    }

    public abstract long getValue();
}
