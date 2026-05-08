package coffeeshout.global.websocket.event;

import coffeeshout.global.metric.WebSocketMetricService;
import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.redis.stream.StreamKey;
import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.global.websocket.PlayerKey;
import coffeeshout.global.websocket.StompSessionManager;
import coffeeshout.global.websocket.event.session.SessionRegisteredEvent;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.service.RoomQueryService;
import java.security.Principal;
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
    private final StompSessionManager sessionManager;
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
        final Principal principal = event.getUser();

        if (principal == null) {
            log.warn("Principal 없음 — roomToken 미검증 연결: sessionId={}", sessionId);
            return;
        }

        final String playerKey = principal.getName();
        if (!PlayerKey.isValid(playerKey)) {
            log.warn("유효하지 않은 Principal 형식: sessionId={}, principal={}", sessionId, playerKey);
            return;
        }

        final PlayerKey parsed = PlayerKey.parse(playerKey);
        log.info("웹소켓 연결 완료: sessionId={}, joinCode={}, playerName={}", sessionId, parsed.joinCode(), parsed.playerName());

        processPlayerConnection(sessionId, parsed.joinCode(), parsed.playerName());
        webSocketMetricService.completeConnection(sessionId);
    }

    private void processPlayerConnection(String sessionId, String joinCode, String playerName) {
        try {
            roomQueryService.getByJoinCode(new JoinCode(joinCode));
            publishSessionRegisteredEvent(sessionId, joinCode, playerName);

        } catch (Exception e) {
            log.warn("플레이어 연결 실패: joinCode={}, playerName={}, error={}", joinCode, playerName, e.getMessage());
        }
    }

    private void publishSessionRegisteredEvent(String sessionId, String joinCode, String playerName) {
        final String playerKey = PlayerKey.of(joinCode, playerName).toString();
        final BaseEvent event = SessionRegisteredEvent.create(playerKey, sessionId);
        streamPublisher.publish(StreamKey.ROOM_BROADCAST, event);

        log.info("세션 등록 이벤트 발행: playerKey={}, sessionId={}", playerKey, sessionId);
    }
}

