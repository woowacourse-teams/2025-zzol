package coffeeshout.laddergame.domain;

import coffeeshout.room.domain.player.PlayerName;

public record LadderLine(PlayerName playerName, int segmentIndex, int row) {
}
