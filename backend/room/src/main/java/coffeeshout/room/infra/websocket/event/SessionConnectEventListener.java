package coffeeshout.room.infra.websocket.event;

import coffeeshout.gamecommon.JoinCode;
import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.room.application.service.RoomQueryService;
import coffeeshout.room.infra.messaging.RoomStreamKey;
import coffeeshout.websocket.PlayerKey;
import coffeeshout.websocket.UserPrincipal;
import coffeeshout.websocket.event.player.PlayerReconnectedEvent;
import coffeeshout.websocket.event.session.SessionRegisteredEvent;
import coffeeshout.websocket.event.user.UserSessionConnectedEvent;
import coffeeshout.websocket.metric.WebSocketMetricService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;
    private final coffeeshout.websocket.StompSessionManager sessionManager;

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

        if (principalName.startsWith(UserPrincipal.PREFIX)) {
            final Long userId = UserPrincipal.extractUserId(event.getUser());
            if (userId != null) {
                sessionManager.registerUserSession(userId, sessionId);
                eventPublisher.publishEvent(new UserSessionConnectedEvent(userId, sessionId));
                log.debug("유저 세션 연결 이벤트 발행: userId={}, sessionId={}", userId, sessionId);
            }
            webSocketMetricService.completeConnection(sessionId);
            return;
        }

        if (!PlayerKey.isValid(principalName)) {
            webSocketMetricService.completeConnection(sessionId);
            return;
        }

        final PlayerKey parsed = PlayerKey.parse(principalName);
        log.info(
                "웹소켓 연결 완료: sessionId={}, joinCode={}, playerName={}",
                sessionId,
                parsed.joinCode(),
                parsed.playerName());
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

        // 재접속 판정을 등록보다 먼저 한다. 순서를 바꾸면 방금 넣은 매핑 때문에 늘 재접속으로 읽힌다
        final boolean reconnected = sessionManager.hasPlayerKeyInternal(playerKey);

        // 소켓을 쥔 인스턴스가 자기 매핑을 즉시 만든다. 스트림을 한 바퀴 돌 때까지 매핑이 없으면
        // 복구 API가 미연결로 오판하고, 연결 직후 끊는 세션은 지울 매핑이 없어 그대로 샌다
        sessionManager.registerPlayerSession(playerKey, sessionId);

        final BaseEvent event = SessionRegisteredEvent.create(playerKey, sessionId);
        streamPublisher.publish(RoomStreamKey.BROADCAST, event);
        log.info("세션 등록 이벤트 발행: playerKey={}, sessionId={}", playerKey, sessionId);

        if (reconnected) {
            log.info("플레이어 재연결 감지: playerKey={}, sessionId={}", playerKey, sessionId);
            streamPublisher.publish(RoomStreamKey.BROADCAST, PlayerReconnectedEvent.create(playerKey, sessionId));
        }
    }
}
