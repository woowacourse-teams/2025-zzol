package coffeeshout.minigame.domain;

import java.util.List;
import java.util.Map;


public interface Playable {

    MiniGameResult getResult();

    Map<Gamer, MiniGameScore> getScores();

    MiniGameType getMiniGameType();

    void setUp(List<Gamer> gamers);

    List<Gamer> getGamers();
}
