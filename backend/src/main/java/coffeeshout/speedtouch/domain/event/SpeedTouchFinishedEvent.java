package coffeeshout.speedtouch.domain.event;

import coffeeshout.speedtouch.domain.SpeedTouchGame;

public record SpeedTouchFinishedEvent(String state, String joinCode) {

    public static SpeedTouchFinishedEvent of(SpeedTouchGame game, String joinCode) {
        return new SpeedTouchFinishedEvent(game.getState().name(), joinCode);
    }
}
