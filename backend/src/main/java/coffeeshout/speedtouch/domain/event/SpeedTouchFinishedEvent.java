package coffeeshout.speedtouch.domain.event;

import coffeeshout.speedtouch.domain.SpeedTouchGame;

public record SpeedTouchFinishedEvent(String joinCode, String state) {

    public static SpeedTouchFinishedEvent of(SpeedTouchGame game, String joinCode) {
        return new SpeedTouchFinishedEvent(joinCode, game.getState().name());
    }
}
