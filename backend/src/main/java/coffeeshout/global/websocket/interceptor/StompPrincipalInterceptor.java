package coffeeshout.global.websocket.interceptor;

import coffeeshout.global.exception.custom.CoffeeShoutException;
import coffeeshout.global.websocket.PlayerKey;
import coffeeshout.user.application.service.AuthTokenService;
import coffeeshout.user.domain.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class StompPrincipalInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthTokenService authTokenService;

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

        verifyTokenIfPresent(accessor);

        accessor.setUser(() -> userName);
        log.debug("STOMP Principal 설정: {}", userName);

        return message;
    }

    private void verifyTokenIfPresent(StompHeaderAccessor accessor) {
        final String authorization = accessor.getFirstNativeHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return;
        }
        final String token = authorization.substring(BEARER_PREFIX.length());
        try {
            final AuthenticatedUser user = authTokenService.verify(token);
            log.debug("STOMP 인증 성공: userId={}", user.userId());
        } catch (CoffeeShoutException e) {
            log.warn("STOMP Access Token 검증 실패: {}", e.getMessage());
        }
    }
}
