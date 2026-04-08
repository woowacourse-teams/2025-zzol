package coffeeshout.room.domain.repository;

import coffeeshout.room.domain.player.Player;
import java.util.List;

public interface PlayerSavePort {

    void saveAll(String joinCode, List<Player> players);
}
