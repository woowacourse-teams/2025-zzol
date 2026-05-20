package coffeeshout.speedtouch.infra.messaging;

import coffeeshout.speedtouch.domain.event.SpeedTouchFinishedEvent;
import coffeeshout.speedtouch.domain.event.SpeedTouchProgressEvent;
import coffeeshout.speedtouch.domain.event.SpeedTouchStateChangedEvent;
import coffeeshout.speedtouch.ui.response.SpeedTouchProgressResponse;
import coffeeshout.speedtouch.ui.response.SpeedTouchStateResponse;
import coffeeshout.websocket.LoggingSimpMessagingTemplate;
import coffeeshout.websocket.docs.WsTopic;
import coffeeshout.websocket.ui.WebSocketResponse;
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
    @WsTopic(path = "/room/{joinCode}/speed-touch/progress", payload = SpeedTouchProgressResponse.class,
            description = "스피드터치 진행 상황 브로드캐스트")
    public void publishProgress(SpeedTouchProgressEvent event) {
        messagingTemplate.convertAndSend(
                String.format(PROGRESS_DESTINATION_FORMAT, event.joinCode()),
                WebSocketResponse.success(SpeedTouchProgressResponse.from(event))
        );
    }

    @EventListener
    @WsTopic(path = "/room/{joinCode}/speed-touch/state", payload = SpeedTouchStateResponse.class,
            description = "스피드터치 게임 상태 변경 브로드캐스트")
    public void publishStateChanged(SpeedTouchStateChangedEvent event) {
        messagingTemplate.convertAndSend(
                String.format(STATE_DESTINATION_FORMAT, event.joinCode()),
                WebSocketResponse.success(new SpeedTouchStateResponse(event.state().name()))
        );
    }

    @EventListener
    @WsTopic(path = "/room/{joinCode}/speed-touch/state", payload = SpeedTouchStateResponse.class,
            description = "스피드터치 게임 종료 브로드캐스트")
    public void publishFinished(SpeedTouchFinishedEvent event) {
        messagingTemplate.convertAndSend(
                String.format(STATE_DESTINATION_FORMAT, event.joinCode()),
                WebSocketResponse.success(new SpeedTouchStateResponse(event.state().name()))
        );
    }
}
