package coffeeshout.room.domain.event.broadcast;

import coffeeshout.room.domain.player.Player;
import java.util.List;

public record PlayerListChangedBroadcast(
        String joinCode,
        List<Player> players) {
}
