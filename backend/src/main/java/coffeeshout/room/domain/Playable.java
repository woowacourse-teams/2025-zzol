package coffeeshout.room.domain;

import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.domain.player.Player;
import java.util.List;
import java.util.Map;

public interface Playable {

    MiniGameResult getResult();

    Map<Player, MiniGameScore> getScores();

    MiniGameType getMiniGameType();

    void setUp(List<Player> players);
}
