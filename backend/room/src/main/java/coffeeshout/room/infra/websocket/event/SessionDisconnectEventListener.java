package coffeeshout.room.infra.websocket.event;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.room.infra.messaging.RoomStreamKey;
import coffeeshout.websocket.StompSessionManager;
import coffeeshout.websocket.SubscriptionInfoService;
import coffeeshout.websocket.event.player.PlayerDisconnectedEvent;
import coffeeshout.websocket.event.user.UserSessionDisconnectedEvent;
import coffeeshout.websocket.metric.WebSocketMetricService;
import coffeeshout.websocket.ratelimit.WebSocketRateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionDisconnectEventListener {

    private static final String CLIENT_DISCONNECT = "CLIENT_DISCONNECT";

    private final StompSessionManager sessionManager;
    private final StreamPublisher streamPublisher;
    private final SubscriptionInfoService subscriptionInfoService;
    private final WebSocketMetricService webSocketMetricService;
    private final WebSocketRateLimiter webSocketRateLimiter;
    private final ApplicationEventPublisher eventPublisher;

    @EventListener
    public void handleSessionDisconnectEvent(SessionDisconnectEvent event) {
        final String sessionId = event.getSessionId();
        final CloseStatus closeStatus = event.getCloseStatus();

        log.info("세션 연결 해제 감지: sessionId={}, closeStatus={}, reason={}",
                sessionId, closeStatus, closeStatus.getReason());

        subscriptionInfoService.removeAllSubscriptions(sessionId);
        webSocketRateLimiter.removeSession(sessionId);

        if (sessionManager.isDisconnectionProcessed(sessionId)) {
            log.debug("이미 처리된 연결 해제 무시: sessionId={}", sessionId);
            return;
        }

        if (sessionManager.hasPlayerKey(sessionId)) {
            final String playerKey = sessionManager.getPlayerKey(sessionId);
            log.info("플레이어 세션 해제 감지: playerKey={}, sessionId={}", playerKey, sessionId);

            final BaseEvent playerDisconnectedEvent = PlayerDisconnectedEvent.create(
                    playerKey, sessionId, "SESSION_DISCONNECT");
            streamPublisher.publish(RoomStreamKey.BROADCAST, playerDisconnectedEvent);
        } else {
            final Long userId = sessionManager.getUserId(sessionId);
            if (userId != null) {
                eventPublisher.publishEvent(new UserSessionDisconnectedEvent(userId, sessionId));
                log.debug("유저 세션 해제 이벤트 발행: userId={}, sessionId={}", userId, sessionId);
            }
        }

        webSocketMetricService.recordDisconnection(sessionId, CLIENT_DISCONNECT);
    }
}
