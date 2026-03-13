package coffeeshout.blindtimer.domain.event;

import coffeeshout.blindtimer.domain.BlindTimerGame;

public record BlindTimerFinishedEvent(String joinCode, String state) {

    public static BlindTimerFinishedEvent of(BlindTimerGame game, String joinCode) {
        return new BlindTimerFinishedEvent(joinCode, game.getState().name());
    }
}
