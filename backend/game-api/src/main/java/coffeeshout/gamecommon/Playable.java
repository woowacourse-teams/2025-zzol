package coffeeshout.gamecommon;

import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import java.util.List;
import java.util.Map;

public interface Playable {

    MiniGameResult getResult();

    Map<Gamer, MiniGameScore> getScores();

    MiniGameType getMiniGameType();

    void setUp(List<Gamer> gamers);
}
