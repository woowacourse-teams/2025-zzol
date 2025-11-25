package coffeeshout.global.websocket.infra.handler;

import coffeeshout.global.websocket.event.session.SessionBaseEvent;
import coffeeshout.global.websocket.event.session.SessionEventType;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionEventHandlerFactory {

    private final List<SessionEventHandler<? extends SessionBaseEvent>> handlers;
    private final Map<SessionEventType, SessionEventHandler<SessionBaseEvent>> handlerMap = new HashMap<>();

    @PostConstruct
    @SuppressWarnings("unchecked")
    public void initialize() {
        for (SessionEventHandler<? extends SessionBaseEvent> handler : handlers) {
            final SessionEventType eventType = handler.getSupportedEventType();
            handlerMap.put(eventType, (SessionEventHandler<SessionBaseEvent>) handler);
            log.info("세션 이벤트 핸들러 등록: eventType={}, handler={}", eventType, handler.getClass().getSimpleName());
        }
    }

    public boolean canHandle(SessionEventType eventType) {
        return handlerMap.containsKey(eventType);
    }

    public SessionEventHandler<SessionBaseEvent> getHandler(SessionEventType eventType) {
        final SessionEventHandler<SessionBaseEvent> handler = handlerMap.get(eventType);
        if (handler == null) {
            throw new IllegalArgumentException("지원하지 않는 이벤트 타입: " + eventType);
        }
        return handler;
    }
}
