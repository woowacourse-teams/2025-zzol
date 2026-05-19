package coffeeshout.room.infra;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.redis.stream.StreamPublisher;
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
        log.info("웹소켓 연결 완료: sessionId={}, joinCode={}, playerName={}", sessionId, parsed.joinCode(), parsed.playerName());
        try {
            processPlayerConnection(sessionId, parsed.joinCode(), parsed.playerName());
        } finally {
            webSocketMetricService.completeConnection(sessionId);
        }
    }

    private void processPlayerConnection(String sessionId, String joinCode, String playerName) {
        try {
            roomQueryService.getByJoinCode(new JoinCode(joinCode));
            publishSessionRegisteredEvent(sessionId, joinCode, playerName);
        } catch (BusinessException e) {
            log.warn("플레이어 연결 실패: joinCode={}, playerName={}, error={}", joinCode, playerName, e.getMessage());
        }
    }

    private void publishSessionRegisteredEvent(String sessionId, String joinCode, String playerName) {
        final String playerKey = PlayerKey.of(joinCode, playerName).toString();
        final BaseEvent event = SessionRegisteredEvent.create(playerKey, sessionId);
        streamPublisher.publish(RoomStreamKey.BROADCAST, event);

        log.info("세션 등록 이벤트 발행: playerKey={}, sessionId={}", playerKey, sessionId);
    }
}
