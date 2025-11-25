package coffeeshout.global.websocket.event;

import coffeeshout.global.metric.WebSocketMetricService;
import coffeeshout.global.websocket.StompSessionManager;
import coffeeshout.global.websocket.event.session.SessionRegisteredEvent;
import coffeeshout.global.websocket.infra.SessionEventPublisher;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.service.RoomQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionConnectEventListener {

    private final WebSocketMetricService webSocketMetricService;
    private final StompSessionManager sessionManager;
    private final SessionEventPublisher sessionEventPublisher;
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

        // simpConnectMessage에서 원래 CONNECT 메시지 가져오기
        final Message<?> connectMessage = (Message<?>) event.getMessage().getHeaders().get("simpConnectMessage");

        if (connectMessage == null) {
            log.warn("simpConnectMessage가 없음: sessionId={}", sessionId);
            return;
        }

        final StompHeaderAccessor accessor = StompHeaderAccessor.wrap(connectMessage);
        final String joinCode = accessor.getFirstNativeHeader("joinCode");
        final String playerName = accessor.getFirstNativeHeader("playerName");

        log.info("웹소켓 연결 완료: sessionId={}, joinCode={}, playerName={}", sessionId, joinCode, playerName);

        // 헤더 정보 검증
        if (joinCode == null || playerName == null) {
            log.warn("헤더 정보 누락: sessionId={}, joinCode={}, playerName={}", sessionId, joinCode, playerName);
            return;
        }

        processPlayerConnection(sessionId, joinCode, playerName);
        webSocketMetricService.completeConnection(sessionId);
    }

    private void processPlayerConnection(String sessionId, String joinCode, String playerName) {
        try {
            // 방 존재 확인
            final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));

            // 게임 중이면 연결 거부
//            if (room.isPlayingState()) {
//                log.info("게임 중인 방 연결 거부: joinCode={}, playerName={}", joinCode, playerName);
//                return;
//            }

            // 세션 등록 이벤트 발행 (모든 인스턴스가 동시에 처리)
            publishSessionRegisteredEvent(sessionId, joinCode, playerName);

        } catch (Exception e) {
            log.warn("플레이어 연결 실패: joinCode={}, playerName={}, error={}", joinCode, playerName, e.getMessage());
        }
    }

    private void publishSessionRegisteredEvent(String sessionId, String joinCode, String playerName) {
        final String playerKey = sessionManager.createPlayerKey(joinCode, playerName);
        final SessionRegisteredEvent event = SessionRegisteredEvent.create(playerKey, sessionId);
        sessionEventPublisher.publishEvent(event);

        log.info("세션 등록 이벤트 발행: playerKey={}, sessionId={}", playerKey, sessionId);
    }
}

