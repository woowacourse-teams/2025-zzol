package coffeeshout.websocket.interceptor;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.websocket.PlayerKey;
import coffeeshout.websocket.auth.RoomSessionClaim;
import coffeeshout.websocket.auth.RoomSessionTokenErrorCode;
import coffeeshout.websocket.auth.RoomSessionTokenService;
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

    private final RoomSessionTokenService roomSessionTokenService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        final StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }

        final String roomToken = accessor.getFirstNativeHeader("roomToken");
        if (roomToken == null) {
            throw new BusinessException(RoomSessionTokenErrorCode.ROOM_TOKEN_MISSING, "roomToken 헤더가 없습니다.");
        }

        final RoomSessionClaim claim = roomSessionTokenService.verify(roomToken);
        final String userName = PlayerKey.of(claim.joinCode(), claim.playerName()).toString();

        accessor.setUser(() -> userName);
        log.debug("STOMP Principal 설정: {}", userName);

        return message;
    }
}
