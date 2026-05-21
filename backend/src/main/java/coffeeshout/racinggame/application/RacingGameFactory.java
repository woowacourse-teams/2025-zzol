package coffeeshout.racinggame.application;

import coffeeshout.gamecommon.MiniGameFactory;
import coffeeshout.gamecommon.Playable;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.racinggame.domain.RacingGame;
import org.springframework.stereotype.Component;

@Component
public class RacingGameFactory implements MiniGameFactory {

    @Override
    public MiniGameType type() {
        return MiniGameType.RACING_GAME;
    }

    @Override
    public Playable create(String joinCode) {
        return new RacingGame();
    }
}
