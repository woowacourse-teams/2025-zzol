package coffeeshout.room.domain.event.broadcast;

import coffeeshout.minigame.domain.MiniGameType;
import java.util.List;

public record MiniGameListChangedBroadcast(
        String joinCode,
        List<MiniGameType> miniGameTypes
) {
}
