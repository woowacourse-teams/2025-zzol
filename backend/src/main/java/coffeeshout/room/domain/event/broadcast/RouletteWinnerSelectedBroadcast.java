package coffeeshout.room.domain.event.broadcast;

import coffeeshout.room.domain.player.Winner;

public record RouletteWinnerSelectedBroadcast(
        String joinCode,
        Winner winner
) {
}
