package coffeeshout.blindtimer.domain.event;

import coffeeshout.blindtimer.domain.BlindTimerGame;
import coffeeshout.blindtimer.domain.BlindTimerGameState;

public record BlindTimerFinishedEvent(String joinCode, BlindTimerGameState state) {

    public static BlindTimerFinishedEvent of(BlindTimerGame game, String joinCode) {
        return new BlindTimerFinishedEvent(joinCode, game.getState());
    }
}
