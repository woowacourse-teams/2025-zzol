package coffeeshout.blindtimer.application;

import coffeeshout.blindtimer.domain.BlindTimerGame;
import coffeeshout.gamecommon.MiniGameFactory;
import coffeeshout.gamecommon.Playable;
import coffeeshout.minigame.domain.MiniGameType;
import org.springframework.stereotype.Component;

@Component
public class BlindTimerGameFactory implements MiniGameFactory {

    @Override
    public MiniGameType type() {
        return MiniGameType.BLIND_TIMER;
    }

    @Override
    public Playable create(String joinCode) {
        return new BlindTimerGame();
    }
}
