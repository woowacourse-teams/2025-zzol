package coffeeshout.bombrelay.infra.messaging;

import coffeeshout.bombrelay.domain.event.BombRelayFinishedEvent;
import coffeeshout.bombrelay.domain.event.BombRelayProgressEvent;
import coffeeshout.bombrelay.domain.event.BombRelayStateChangedEvent;
import coffeeshout.bombrelay.domain.event.WordResultEvent;
import coffeeshout.bombrelay.ui.response.BombRelayProgressResponse;
import coffeeshout.bombrelay.ui.response.BombRelayStateResponse;
import coffeeshout.bombrelay.ui.response.WordResultResponse;
import coffeeshout.global.websocket.LoggingSimpMessagingTemplate;
import coffeeshout.global.websocket.ui.WebSocketResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BombRelayGameMessagePublisher {

    private static final String STATE_DESTINATION_FORMAT = "/topic/room/%s/bomb-relay/state";
    private static final String PROGRESS_DESTINATION_FORMAT = "/topic/room/%s/bomb-relay/progress";
    private static final String WORD_RESULT_DESTINATION_FORMAT = "/topic/room/%s/bomb-relay/word-result";

    private final LoggingSimpMessagingTemplate messagingTemplate;

    @EventListener
    public void publishStateChanged(BombRelayStateChangedEvent event) {
        messagingTemplate.convertAndSend(
                String.format(STATE_DESTINATION_FORMAT, event.joinCode()),
                WebSocketResponse.success(BombRelayStateResponse.from(event))
        );
    }

    @EventListener
    public void publishProgress(BombRelayProgressEvent event) {
        messagingTemplate.convertAndSend(
                String.format(PROGRESS_DESTINATION_FORMAT, event.joinCode()),
                WebSocketResponse.success(BombRelayProgressResponse.from(event))
        );
    }

    @EventListener
    public void publishWordResult(WordResultEvent event) {
        messagingTemplate.convertAndSend(
                String.format(WORD_RESULT_DESTINATION_FORMAT, event.joinCode()),
                WebSocketResponse.success(WordResultResponse.from(event))
        );
    }

    @EventListener
    public void publishFinished(BombRelayFinishedEvent event) {
        messagingTemplate.convertAndSend(
                String.format(STATE_DESTINATION_FORMAT, event.joinCode()),
                WebSocketResponse.success(new BombRelayStateResponse(event.state(), 0, 0, null, null, null))
        );
    }
}
