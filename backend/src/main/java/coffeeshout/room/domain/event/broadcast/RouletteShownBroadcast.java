package coffeeshout.room.domain.event.broadcast;

import coffeeshout.room.domain.RoomState;

public record RouletteShownBroadcast(
        String joinCode,
        RoomState roomState
) {
}
