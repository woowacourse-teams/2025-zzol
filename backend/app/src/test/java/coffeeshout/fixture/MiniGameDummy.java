package coffeeshout.fixture;

import coffeeshout.cardgame.domain.CardGameScore;
import coffeeshout.minigame.domain.Gamer;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.domain.Playable;
import coffeeshout.room.domain.player.PlayerName;
import java.util.List;
import java.util.Map;

public class MiniGameDummy implements Playable {

    @Override
    public MiniGameResult getResult() {
        return MiniGameResult.fromDescending(getScores());
    }

    @Override
    public Map<PlayerName, MiniGameScore> getScores() {
        return Map.of(
                new PlayerName("꾹이"), new CardGameScore(20),
                new PlayerName("루키"), new CardGameScore(-10)
        );
    }

    @Override
    public MiniGameType getMiniGameType() {
        return MiniGameType.CARD_GAME;
    }

    @Override
    public void setUp(List<Gamer> gamers) {
    }
}
