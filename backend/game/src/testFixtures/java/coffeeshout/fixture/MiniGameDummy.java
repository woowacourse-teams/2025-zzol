package coffeeshout.fixture;

import coffeeshout.cardgame.domain.CardGameScore;
import coffeeshout.gamecommon.MiniGameFactory;
import coffeeshout.gamecommon.Playable;
import coffeeshout.gamecommon.PlayerView;
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
    public Map<PlayerView, MiniGameScore> getScores() {
        final Map<PlayerView, MiniGameScore> scores = new HashMap<>();
        scores.put(PlayerFixture.호스트꾹이(), new CardGameScore(20));
        scores.put(PlayerFixture.게스트루키(), new CardGameScore(-10));
        return scores;
    }

    @Override
    public MiniGameType getMiniGameType() {
        return MiniGameType.CARD_GAME;
    }

    @Override
    public void setUp(List<? extends PlayerView> players) {
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
