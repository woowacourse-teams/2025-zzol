package coffeeshout.blindtimer.domain.event;

import coffeeshout.blindtimer.domain.BlindTimerGame;
import coffeeshout.blindtimer.domain.BlindTimerGameState;
import java.time.Duration;

public record BlindTimerStateChangedEvent(
        String joinCode,
        BlindTimerGameState state,
        Duration targetTime,
        Duration blindDelay
) {

    public static BlindTimerStateChangedEvent of(BlindTimerGame game, String joinCode, Duration blindDelay) {
        return new BlindTimerStateChangedEvent(
                joinCode,
                game.getState(),
                game.getTargetTime(),
                blindDelay
        );
    }
}
