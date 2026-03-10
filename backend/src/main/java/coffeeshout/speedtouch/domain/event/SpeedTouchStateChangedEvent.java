package coffeeshout.speedtouch.domain.event;

import coffeeshout.speedtouch.domain.SpeedTouchGame;

public record SpeedTouchStateChangedEvent(String joinCode, String state) {

    public static SpeedTouchStateChangedEvent of(SpeedTouchGame game, String joinCode) {
        return new SpeedTouchStateChangedEvent(joinCode, game.getState().name());
    }
}
