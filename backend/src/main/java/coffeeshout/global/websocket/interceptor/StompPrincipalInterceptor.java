package coffeeshout.global.websocket.interceptor;

import coffeeshout.global.websocket.PlayerKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StompPrincipalInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }

        String joinCode = accessor.getFirstNativeHeader("joinCode");
        String playerName = accessor.getFirstNativeHeader("playerName");

        String userName;
        if (joinCode == null || playerName == null) {
            log.warn("STOMP CONNECT 헤더 누락으로 sessionId를 Principal로 사용: sessionId={}", accessor.getSessionId());
            userName = accessor.getSessionId();
        } else {
            userName = PlayerKey.of(joinCode, playerName).toString();
        }

        accessor.setUser(() -> userName);
        log.debug("STOMP Principal 설정: {}", userName);

        return message;
    }
}
