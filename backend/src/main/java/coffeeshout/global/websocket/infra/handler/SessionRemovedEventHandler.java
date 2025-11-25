package coffeeshout.global.websocket.infra.handler;

import coffeeshout.global.websocket.StompSessionManager;
import coffeeshout.global.websocket.event.session.SessionEventType;
import coffeeshout.global.websocket.event.session.SessionRemovedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionRemovedEventHandler implements SessionEventHandler<SessionRemovedEvent> {

    private final StompSessionManager sessionManager;

    @Override
    public void handle(SessionRemovedEvent event) {
        final String sessionId = event.sessionId();
        
        log.info("세션 제거 이벤트 처리: sessionId={}", sessionId);

        // 모든 인스턴스가 세션 매핑 제거
        sessionManager.removeSessionInternal(sessionId);
    }

    @Override
    public SessionEventType getSupportedEventType() {
        return SessionEventType.SESSION_REMOVED;
    }
}
