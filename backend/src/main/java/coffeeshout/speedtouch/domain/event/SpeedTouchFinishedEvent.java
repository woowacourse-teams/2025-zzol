package coffeeshout.speedtouch.domain.event;

import coffeeshout.speedtouch.domain.SpeedTouchGame;
import coffeeshout.speedtouch.domain.SpeedTouchGameState;

public record SpeedTouchFinishedEvent(String joinCode, SpeedTouchGameState state) {

    public static SpeedTouchFinishedEvent of(SpeedTouchGame game, String joinCode) {
        return new SpeedTouchFinishedEvent(joinCode, game.getState());
    }
}
