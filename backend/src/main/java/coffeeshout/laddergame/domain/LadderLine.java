package coffeeshout.laddergame.domain;

import coffeeshout.room.domain.player.PlayerName;
import java.util.Objects;

public record LadderLine(PlayerName playerName, int segmentIndex, int row) {

    public LadderLine {
        Objects.requireNonNull(playerName, "playerName must not be null");
        if (segmentIndex < 0) {
            throw new IllegalArgumentException("segmentIndex must be >= 0: " + segmentIndex);
        }
        if (row < 1) {
            throw new IllegalArgumentException("row must be >= 1: " + row);
        }
    }
}
