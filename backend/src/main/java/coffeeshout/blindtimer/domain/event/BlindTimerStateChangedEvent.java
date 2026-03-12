package coffeeshout.blindtimer.domain.event;

import coffeeshout.blindtimer.domain.BlindTimerGame;

public record BlindTimerStateChangedEvent(
        String joinCode,
        String state,
        long targetTimeMillis,
        long blindDelayMillis
) {

    public static BlindTimerStateChangedEvent of(BlindTimerGame game, String joinCode, long blindDelayMillis) {
        return new BlindTimerStateChangedEvent(
                joinCode,
                game.getState().name(),
                game.getTargetTimeMillis(),
                blindDelayMillis
        );
    }
}
