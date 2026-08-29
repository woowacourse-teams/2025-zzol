package coffeeshout.websocket;

import coffeeshout.global.notify.NotificationSink;
import coffeeshout.websocket.ui.WebSocketResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 알림 채널이 수신한 메시지를 이 인스턴스의 STOMP 클라이언트로 전달한다.
 * <p>
 * 페이로드를 {@link WebSocketResponse}로 <b>되살려서</b> 넘기는 것이 핵심이다.
 * {@link LoggingSimpMessagingTemplate#convertAndSend}는 payload가 {@code WebSocketResponse}이고
 * destination이 방 패턴({@code /topic/room/{joinCode}/...})에 맞을 때만 복구 저장 경로를 탄다. 문자열을
 * 그대로 넘기면 두 조건 모두 어긋나 저장 없이 통과하고, 재접속 클라이언트가 이 알림을 복구하지 못한다.
 * {@code WsRecoveryService.deserializeMessage()}가 복구 조회에서 하는 역직렬화와 같은 처리다.
 * <p>
 * 전 인스턴스가 같은 메시지를 받아 각자 저장을 시도하지만 중복은 쌓이지 않는다 —
 * {@code WsRecoveryService}의 메시지 식별자는 (destination + success + data + errorMessage)의 md5라
 * 인스턴스마다 동일하게 나오고, 저장 Lua 스크립트가 이미 있는 식별자면 기존 streamId를 그대로 돌려준다.
 * id 필드는 식별자 계산에 들어가지 않으므로 저장 순서도 결과를 바꾸지 않는다.
 */
@Slf4j
@Component
public class WsNotificationSink implements NotificationSink {

    private final LoggingSimpMessagingTemplate loggingSimpMessagingTemplate;
    private final ObjectMapper objectMapper;

    public WsNotificationSink(
            LoggingSimpMessagingTemplate loggingSimpMessagingTemplate,
            @Qualifier("redisObjectMapper") ObjectMapper objectMapper
    ) {
        this.loggingSimpMessagingTemplate = loggingSimpMessagingTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void deliver(String destination, String payloadJson) {
        try {
            final WebSocketResponse<?> response = objectMapper.readValue(payloadJson, WebSocketResponse.class);
            loggingSimpMessagingTemplate.convertAndSend(destination, response);
        } catch (JsonProcessingException e) {
            log.error("알림 페이로드 역직렬화 실패 — destination: {}", destination, e);
        }
    }
}
