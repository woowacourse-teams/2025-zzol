package coffeeshout.global.exception;

import coffeeshout.global.exception.custom.CoffeeShoutException;
import coffeeshout.global.websocket.LoggingSimpMessagingTemplate;
import java.security.Principal;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
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

    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    public void handleValidationException(
            MethodArgumentNotValidException e,
            Principal user,
            @Header("simpDestination") String destination
    ) {
        final String errors = e.getBindingResult().getAllErrors().stream()
                .map(error -> error instanceof FieldError fe
                        ? fe.getField() + ": " + fe.getDefaultMessage()
                        : (error.getCode() != null ? error.getCode() : error.getObjectName()) + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        final String message = errors.isBlank() ? "입력값이 유효하지 않습니다." : errors;
        log.warn("WebSocket ValidationException: destination={}, errors={}", destination, message);

        messagingTemplate.convertAndSendError(user.getName(), message);
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
