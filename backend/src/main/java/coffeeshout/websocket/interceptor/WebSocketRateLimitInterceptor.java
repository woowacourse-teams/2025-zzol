package coffeeshout.websocket.interceptor;

import coffeeshout.websocket.ratelimit.WebSocketRateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * WebSocket inbound 메시지 Rate Limiting 인터셉터
 * <p>
 * 세션별로 초당 메시지 수를 제한한다.
 * HTTP Rate Limiting은 Nginx에서 처리하고, WebSocket 메시지는 여기서 처리한다.
 * <p>
 * Nginx가 WebSocket 메시지를 제한할 수 없는 이유:
 * Nginx는 WebSocket 핸드셰이크(HTTP Upgrade)만 제한 가능하고,
 * 핸드셰이크 이후 TCP 스트림 위의 STOMP 메시지 내용은 파싱하지 못한다.
 * <p>
 * Rate Limit 초과 시 메시지를 드롭(null 반환)한다.
 * 클라이언트에 에러 메시지를 보내지 않는 이유:
 * 에러 응답 자체가 outbound 스레드를 점유하므로, 초당 수십 건의 초과 메시지마다
 * 에러를 보내면 서버 보호라는 목적에 역행한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketRateLimitInterceptor implements ChannelInterceptor {

    private final WebSocketRateLimiter rateLimiter;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        final StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                message, StompHeaderAccessor.class
        );

        if (accessor == null) {
            return message;
        }

        final StompCommand command = accessor.getCommand();

        // SEND 메시지만 Rate Limiting 대상
        // CONNECT, SUBSCRIBE, DISCONNECT 등은 제한하지 않음
        if (command != StompCommand.SEND) {
            return message;
        }

        final String sessionId = accessor.getSessionId();
        if (sessionId == null) {
            return message;
        }

        if (!rateLimiter.tryAcquire(sessionId)) {
            log.warn("WebSocket Rate Limit 초과: sessionId={}, destination={}",
                    sessionId, accessor.getDestination());
            return null; // 메시지 드롭
        }

        return message;
    }
}
