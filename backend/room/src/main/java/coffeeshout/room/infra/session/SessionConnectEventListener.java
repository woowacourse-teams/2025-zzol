package coffeeshout.room.infra.session;

import coffeeshout.exception.custom.BusinessException;
import coffeeshout.redis.BaseEvent;
import coffeeshout.redis.stream.StreamPublisher;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.service.RoomQueryService;
import coffeeshout.room.infra.messaging.RoomStreamKey;
import coffeeshout.websocket.PlayerKey;
import coffeeshout.websocket.UserPrincipal;
import coffeeshout.websocket.event.session.SessionRegisteredEvent;
import coffeeshout.websocket.metric.WebSocketMetricService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionConnectEventListener {

    private final WebSocketMetricService webSocketMetricService;
    private final StreamPublisher streamPublisher;
    private final RoomQueryService roomQueryService;

    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        final String sessionId = event.getMessage().getHeaders().get("simpSessionId", String.class);
        log.info("웹소켓 연결 시작: sessionId={}", sessionId);
        webSocketMetricService.startConnection(sessionId);
    }

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        final String sessionId = event.getMessage().getHeaders().get("simpSessionId", String.class);
        if (event.getUser() == null) {
            webSocketMetricService.completeConnection(sessionId);
            return;
        }
        final String principalName = event.getUser().getName();

        if (principalName.startsWith(UserPrincipal.PREFIX) || !PlayerKey.isValid(principalName)) {
            webSocketMetricService.completeConnection(sessionId);
            return;
        }

        final PlayerKey parsed = PlayerKey.parse(principalName);
        log.info("웹소켓 연결 완료: sessionId={}, joinCode={}, playerName={}, userId={}", sessionId, parsed.joinCode(), parsed.playerName(), parsed.userId());
        try {
            processPlayerConnection(sessionId, parsed);
        } finally {
            webSocketMetricService.completeConnection(sessionId);
        }
    }

    private void processPlayerConnection(String sessionId, PlayerKey playerKey) {
        try {
            roomQueryService.getByJoinCode(new JoinCode(playerKey.joinCode()));
            publishSessionRegisteredEvent(sessionId, playerKey);
        } catch (BusinessException e) {
            log.warn("플레이어 연결 실패: joinCode={}, playerName={}, error={}", playerKey.joinCode(), playerKey.playerName(), e.getMessage());
        }
    }

    private void publishSessionRegisteredEvent(String sessionId, PlayerKey playerKey) {
        final String playerKeyStr = playerKey.toString();
        final BaseEvent event = SessionRegisteredEvent.create(playerKeyStr, sessionId);
        streamPublisher.publish(RoomStreamKey.BROADCAST, event);

        log.info("세션 등록 이벤트 발행: playerKey={}, sessionId={}", playerKeyStr, sessionId);
    }
}
