package coffeeshout.blindtimer.ui.response;

import coffeeshout.blindtimer.domain.event.BlindTimerStateChangedEvent;

public record BlindTimerStateResponse(
        String state,
        long targetTimeMillis,
        long blindDelayMillis
) {

    public static BlindTimerStateResponse from(BlindTimerStateChangedEvent event) {
        return new BlindTimerStateResponse(
                event.state().name(),
                event.targetTime().toMillis(),
                event.blindDelay().toMillis()
        );
    }
}
