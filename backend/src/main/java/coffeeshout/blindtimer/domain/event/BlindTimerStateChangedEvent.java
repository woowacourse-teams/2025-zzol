package coffeeshout.blindtimer.domain.event;

import coffeeshout.blindtimer.domain.BlindTimerGame;
import java.time.Duration;

public record BlindTimerStateChangedEvent(
        String joinCode,
        String state,
        Duration targetTime,
        Duration blindDelay
) {

    public static BlindTimerStateChangedEvent of(BlindTimerGame game, String joinCode, Duration blindDelay) {
        return new BlindTimerStateChangedEvent(
                joinCode,
                game.getState().name(),
                game.getTargetTime(),
                blindDelay
        );
    }
}
