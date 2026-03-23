package coffeeshout.speedtouch.domain.event;

import coffeeshout.speedtouch.domain.SpeedTouchGame;
import coffeeshout.speedtouch.domain.SpeedTouchGameState;

public record SpeedTouchStateChangedEvent(String joinCode, SpeedTouchGameState state) {

    public static SpeedTouchStateChangedEvent of(SpeedTouchGame game, String joinCode) {
        return new SpeedTouchStateChangedEvent(joinCode, game.getState());
    }
}
