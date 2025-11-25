package coffeeshout.global.exception;

import coffeeshout.global.ui.WebSocketResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
@RequiredArgsConstructor
public class WebSocketExceptionHandler {

    private final SimpMessagingTemplate messagingTemplate;

    @MessageExceptionHandler(Exception.class)
    public void handleException(
            Exception e,
            @Header("simpSessionId") String sessionId,
            @Header("simpDestination") String destination
    ) {

        messagingTemplate.convertAndSendToUser(
                sessionId,
                "/queue/errors",
                WebSocketResponse.error("처리 중 오류가 발생했습니다.")
        );
    }
}
