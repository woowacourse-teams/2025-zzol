package coffeeshout.blindtimer.ui.response;

import coffeeshout.blindtimer.domain.BlindTimerGameState;
import coffeeshout.blindtimer.domain.event.BlindTimerStateChangedEvent;

public record BlindTimerStateResponse(BlindTimerGameState state, long targetTimeMillis, long blindDelayMillis) {

    public static BlindTimerStateResponse from(BlindTimerStateChangedEvent event) {
        return new BlindTimerStateResponse(
                event.state(), event.targetTime().toMillis(), event.blindDelay().toMillis());
    }
}
