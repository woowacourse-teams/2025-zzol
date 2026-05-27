package coffeeshout.speedtouch.ui.response;

import coffeeshout.speedtouch.domain.event.SpeedTouchProgressEvent;
import coffeeshout.speedtouch.domain.event.SpeedTouchProgressEvent.SpeedTouchPlayerProgress;
import java.util.List;

public record SpeedTouchProgressResponse(List<SpeedTouchPlayerProgress> players) {

    public static SpeedTouchProgressResponse from(SpeedTouchProgressEvent event) {
        return new SpeedTouchProgressResponse(event.players());
    }
}
