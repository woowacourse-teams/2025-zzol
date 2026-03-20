package coffeeshout.speedtouch.infra.messaging;

import coffeeshout.global.websocket.LoggingSimpMessagingTemplate;
import coffeeshout.global.websocket.ui.WebSocketResponse;
import coffeeshout.speedtouch.domain.event.SpeedTouchFinishedEvent;
import coffeeshout.speedtouch.domain.event.SpeedTouchProgressEvent;
import coffeeshout.speedtouch.domain.event.SpeedTouchStateChangedEvent;
import coffeeshout.speedtouch.ui.response.SpeedTouchProgressResponse;
import coffeeshout.speedtouch.ui.response.SpeedTouchStateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpeedTouchGameMessagePublisher {

    private static final String PROGRESS_DESTINATION_FORMAT = "/topic/room/%s/speed-touch/progress";
    private static final String STATE_DESTINATION_FORMAT = "/topic/room/%s/speed-touch/state";

    private final LoggingSimpMessagingTemplate messagingTemplate;

    @EventListener
    public void publishProgress(SpeedTouchProgressEvent event) {
        messagingTemplate.convertAndSend(
                String.format(PROGRESS_DESTINATION_FORMAT, event.joinCode()),
                WebSocketResponse.success(SpeedTouchProgressResponse.from(event))
        );
    }

    @EventListener
    public void publishStateChanged(SpeedTouchStateChangedEvent event) {
        messagingTemplate.convertAndSend(
                String.format(STATE_DESTINATION_FORMAT, event.joinCode()),
                WebSocketResponse.success(new SpeedTouchStateResponse(event.state().name()))
        );
    }

    @EventListener
    public void publishFinished(SpeedTouchFinishedEvent event) {
        messagingTemplate.convertAndSend(
                String.format(STATE_DESTINATION_FORMAT, event.joinCode()),
                WebSocketResponse.success(new SpeedTouchStateResponse(event.state().name()))
        );
    }
}
