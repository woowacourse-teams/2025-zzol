package coffeeshout.fixture;

import coffeeshout.cardgame.domain.CardGameScore;
import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.MiniGameFactory;
import coffeeshout.gamecommon.Playable;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MiniGameDummy implements Playable {

    @Override
    public MiniGameResult getResult() {
        return MiniGameResult.fromDescending(getScores());
    }

    @Override
    public Map<Gamer, MiniGameScore> getScores() {
        final Map<Gamer, MiniGameScore> scores = new HashMap<>();
        scores.put(Gamer.of("꾹이", null), new CardGameScore(20));
        scores.put(Gamer.of("루키", null), new CardGameScore(-10));
        return scores;
    }

    @Override
    public MiniGameType getMiniGameType() {
        return MiniGameType.CARD_GAME;
    }

    @Override
    public void setUp(List<Gamer> gamers) {
    }

    public static class Factory implements MiniGameFactory {
        @Override
        public MiniGameType type() {
            return MiniGameType.CARD_GAME;
        }

        @Override
        public Playable create(String joinCode) {
            return new MiniGameDummy();
        }
    }
}
