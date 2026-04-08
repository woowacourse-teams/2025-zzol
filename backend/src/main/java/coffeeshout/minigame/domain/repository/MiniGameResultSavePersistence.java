package coffeeshout.minigame.domain.repository;

import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.domain.player.Player;
import java.util.List;
import java.util.Map;

public interface MiniGameResultSavePersistence {

    void saveResults(String joinCode, MiniGameType miniGameType,
                     List<Player> players, MiniGameResult result, Map<Player, MiniGameScore> scores);
}
