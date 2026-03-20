package coffeeshout.global.exception;

import coffeeshout.global.exception.custom.CoffeeShoutException;
import coffeeshout.global.websocket.LoggingSimpMessagingTemplate;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class WebSocketExceptionHandler {

    private final LoggingSimpMessagingTemplate messagingTemplate;

    @MessageExceptionHandler(CoffeeShoutException.class)
    public void handleBusinessException(
            CoffeeShoutException e,
            Principal user,
            @Header("simpDestination") String destination
    ) {
        log.warn("WebSocket BusinessException: destination={}, errorCode={}, message={}",
                destination, e.getErrorCode().getCode(), e.getMessage());

        messagingTemplate.convertAndSendError(
                user.getName(),
                e.getErrorCode().getMessage()
        );
    }

    @MessageExceptionHandler(Exception.class)
    public void handleException(
            Exception e,
            Principal user,
            @Header("simpDestination") String destination
    ) {
        log.error("WebSocket Exception: destination={}, message={}", destination, e.getMessage(), e);

        messagingTemplate.convertAndSendError(
                user.getName(),
                "처리 중 오류가 발생했습니다."
        );
    }
}
