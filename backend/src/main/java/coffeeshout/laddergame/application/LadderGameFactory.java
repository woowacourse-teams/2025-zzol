package coffeeshout.laddergame.application;

import coffeeshout.gamecommon.MiniGameFactory;
import coffeeshout.gamecommon.Playable;
import coffeeshout.laddergame.domain.LadderGame;
import coffeeshout.minigame.domain.MiniGameType;
import org.springframework.stereotype.Component;

@Component
public class LadderGameFactory implements MiniGameFactory {

    @Override
    public MiniGameType type() {
        return MiniGameType.LADDER_GAME;
    }

    @Override
    public Playable create(String joinCode) {
        return new LadderGame();
    }
}
