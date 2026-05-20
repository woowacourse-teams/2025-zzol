package coffeeshout.blindtimer.ui.response;

import coffeeshout.blindtimer.domain.event.BlindTimerProgressEvent;
import coffeeshout.blindtimer.domain.event.BlindTimerProgressEvent.PlayerProgress;
import java.util.List;

public record BlindTimerProgressResponse(List<PlayerProgress> players) {

    public static BlindTimerProgressResponse from(BlindTimerProgressEvent event) {
        return new BlindTimerProgressResponse(event.players());
    }
}
