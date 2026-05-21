package coffeeshout.user.websocket.interceptor;

import coffeeshout.exception.custom.BusinessException;
import coffeeshout.websocket.PlayerKey;
import coffeeshout.websocket.UserPrincipal;
import coffeeshout.websocket.auth.RoomSessionClaim;
import coffeeshout.websocket.auth.RoomSessionTokenService;
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

    private final RoomSessionTokenService roomSessionTokenService;
    private final AuthTokenService authTokenService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        final StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }

        final String roomToken = accessor.getFirstNativeHeader("roomToken");
        if (roomToken != null) {
            final RoomSessionClaim claim = roomSessionTokenService.verify(roomToken);
            final String playerKey = PlayerKey.of(claim.joinCode(), claim.playerName(), claim.userId()).toString();
            accessor.setUser(() -> playerKey);
            log.debug("STOMP Room Principal 설정: {}", playerKey);
            return message;
        }

        final String userName = resolveUserPrincipal(accessor);
        accessor.setUser(() -> userName);
        log.debug("STOMP User Principal 설정: {}", userName);
        return message;
    }

    private String resolveUserPrincipal(StompHeaderAccessor accessor) {
        final String authorization = accessor.getFirstNativeHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            log.warn("STOMP CONNECT 헤더 누락으로 sessionId를 Principal로 사용: sessionId={}", accessor.getSessionId());
            return accessor.getSessionId();
        }
        final String token = authorization.substring(BEARER_PREFIX.length());
        try {
            final AuthenticatedUser user = authTokenService.verify(token);
            log.debug("STOMP 사용자 인증 성공: userId={}", user.userId());
            return UserPrincipal.of(user.userId());
        } catch (BusinessException e) {
            log.warn("STOMP Access Token 검증 실패: {}", e.getMessage());
            return accessor.getSessionId();
        }
    }
}
