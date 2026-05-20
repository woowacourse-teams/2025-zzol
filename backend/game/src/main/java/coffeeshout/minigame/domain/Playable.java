package coffeeshout.minigame.domain;

import coffeeshout.room.domain.player.PlayerName;
import java.util.List;
import java.util.Map;

public interface Playable {

    MiniGameResult getResult();

    Map<PlayerName, MiniGameScore> getScores();

    MiniGameType getMiniGameType();

    void setUp(List<PlayerName> players);
}
