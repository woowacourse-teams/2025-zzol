package coffeeshout.blindtimer.infra.messaging;

import coffeeshout.blindtimer.domain.event.BlindTimerFinishedEvent;
import coffeeshout.blindtimer.domain.event.BlindTimerProgressEvent;
import coffeeshout.blindtimer.domain.event.BlindTimerStateChangedEvent;
import coffeeshout.blindtimer.ui.response.BlindTimerProgressResponse;
import coffeeshout.blindtimer.ui.response.BlindTimerStateResponse;
import coffeeshout.global.websocket.LoggingSimpMessagingTemplate;
import coffeeshout.global.websocket.docs.WsTopic;
import coffeeshout.global.websocket.ui.WebSocketResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BlindTimerGameMessagePublisher {

    private static final String PROGRESS_DESTINATION_FORMAT = "/topic/room/%s/blind-timer/progress";
    private static final String STATE_DESTINATION_FORMAT = "/topic/room/%s/blind-timer/state";

    private final LoggingSimpMessagingTemplate messagingTemplate;

    @EventListener
    @WsTopic(path = "/room/{joinCode}/blind-timer/progress", payload = BlindTimerProgressResponse.class,
            description = "블라인드 타이머 진행 상황 브로드캐스트")
    public void publishProgress(BlindTimerProgressEvent event) {
        messagingTemplate.convertAndSend(
                String.format(PROGRESS_DESTINATION_FORMAT, event.joinCode()),
                WebSocketResponse.success(BlindTimerProgressResponse.from(event))
        );
    }

    @EventListener
    @WsTopic(path = "/room/{joinCode}/blind-timer/state", payload = BlindTimerStateResponse.class,
            description = "블라인드 타이머 상태 변경 브로드캐스트")
    public void publishStateChanged(BlindTimerStateChangedEvent event) {
        messagingTemplate.convertAndSend(
                String.format(STATE_DESTINATION_FORMAT, event.joinCode()),
                WebSocketResponse.success(BlindTimerStateResponse.from(event))
        );
    }

    @EventListener
    @WsTopic(path = "/room/{joinCode}/blind-timer/state", payload = BlindTimerStateResponse.class,
            description = "블라인드 타이머 종료 브로드캐스트")
    public void publishFinished(BlindTimerFinishedEvent event) {
        messagingTemplate.convertAndSend(
                String.format(STATE_DESTINATION_FORMAT, event.joinCode()),
                WebSocketResponse.success(new BlindTimerStateResponse(event.state().name(), 0, 0))
        );
    }
}
