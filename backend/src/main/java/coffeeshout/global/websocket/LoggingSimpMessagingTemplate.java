package coffeeshout.global.websocket;

import coffeeshout.global.ui.WebSocketResponse;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class LoggingSimpMessagingTemplate {

    private final SimpMessagingTemplate messagingTemplate;

    @Observed(name = "websocket.send")
    public void convertAndSend(String destination, Object payload) {
        messagingTemplate.convertAndSend(destination, payload);
    }

    @Observed(name = "websocket.send.toUser")
    public void convertAndSendToUser(String sessionId, String destination, Object payload) {
        messagingTemplate.convertAndSendToUser(sessionId, destination, payload);
    }

    @Observed(name = "websocket.send.error")
    public void convertAndSendError(String sessionId, String errorMessage) {
        messagingTemplate.convertAndSendToUser(sessionId, "/queue/errors", WebSocketResponse.error(errorMessage));
    }
}
