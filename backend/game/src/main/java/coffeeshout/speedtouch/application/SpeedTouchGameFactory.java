package coffeeshout.speedtouch.application;

import coffeeshout.gamecommon.MiniGameFactory;
import coffeeshout.gamecommon.Playable;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.speedtouch.domain.SpeedTouchGame;
import org.springframework.stereotype.Component;

@Component
public class SpeedTouchGameFactory implements MiniGameFactory {

    @Override
    public MiniGameType type() {
        return MiniGameType.SPEED_TOUCH;
    }

    @Override
    public Playable create(String joinCode) {
        return new SpeedTouchGame();
    }
}
