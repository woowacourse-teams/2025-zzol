package coffeeshout.minigame.domain;

import coffeeshout.room.domain.player.Player;
import java.util.List;
import java.util.Map;

public interface Playable {

    MiniGameResult getResult();

    Map<Player, MiniGameScore> getScores();

    MiniGameType getMiniGameType();

    void setUp(List<Player> players);
}
