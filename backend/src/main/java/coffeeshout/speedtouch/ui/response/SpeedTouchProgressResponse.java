package coffeeshout.speedtouch.ui.response;

import coffeeshout.speedtouch.domain.event.SpeedTouchProgressEvent;
import coffeeshout.speedtouch.domain.event.SpeedTouchProgressEvent.PlayerProgress;
import java.util.List;

public record SpeedTouchProgressResponse(List<PlayerProgress> players) {

    public static SpeedTouchProgressResponse from(SpeedTouchProgressEvent event) {
        return new SpeedTouchProgressResponse(event.players());
    }
}
