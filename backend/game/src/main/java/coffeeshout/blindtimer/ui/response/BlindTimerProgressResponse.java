package coffeeshout.blindtimer.ui.response;

import coffeeshout.blindtimer.domain.event.BlindTimerProgressEvent;
import coffeeshout.blindtimer.domain.event.BlindTimerProgressEvent.BlindTimerPlayerProgress;
import java.util.List;

public record BlindTimerProgressResponse(List<BlindTimerPlayerProgress> players) {

    public static BlindTimerProgressResponse from(BlindTimerProgressEvent event) {
        return new BlindTimerProgressResponse(event.players());
    }
}
