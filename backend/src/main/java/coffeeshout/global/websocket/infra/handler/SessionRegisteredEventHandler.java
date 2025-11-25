package coffeeshout.global.websocket.infra.handler;

import coffeeshout.global.websocket.StompSessionManager;
import coffeeshout.global.websocket.event.player.PlayerReconnectedEvent;
import coffeeshout.global.websocket.event.session.SessionEventType;
import coffeeshout.global.websocket.event.session.SessionRegisteredEvent;
import coffeeshout.global.websocket.infra.PlayerEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionRegisteredEventHandler implements SessionEventHandler<SessionRegisteredEvent> {

    private final StompSessionManager sessionManager;
    private final PlayerEventPublisher playerEventPublisher;

    @Override
    public void handle(SessionRegisteredEvent event) {
        final String playerKey = event.playerKey();
        final String sessionId = event.sessionId();
        
        log.info("세션 등록 이벤트 처리: playerKey={}, sessionId={}", playerKey, sessionId);

        // 기존 세션이 있으면 재연결 처리
        if (sessionManager.hasPlayerKeyInternal(playerKey)) {
            log.info("플레이어 재연결 감지: playerKey={}, sessionId={}", playerKey, sessionId);
            
            // 플레이어 재연결 이벤트 발행
            final PlayerReconnectedEvent playerReconnectedEvent = PlayerReconnectedEvent.create(playerKey, sessionId);
            playerEventPublisher.publishEvent(playerReconnectedEvent);
        }

        // 모든 인스턴스가 세션 매핑 등록
        sessionManager.registerPlayerSessionInternal(playerKey, sessionId);
    }

    @Override
    public SessionEventType getSupportedEventType() {
        return SessionEventType.SESSION_REGISTERED;
    }
}
