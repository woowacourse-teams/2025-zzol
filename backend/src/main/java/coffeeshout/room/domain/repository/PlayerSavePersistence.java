package coffeeshout.room.domain.repository;

import coffeeshout.room.domain.player.Player;
import java.util.List;

public interface PlayerSavePersistence {

    void saveAll(String joinCode, List<Player> players);
}
