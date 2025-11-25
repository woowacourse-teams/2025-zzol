package coffeeshout.global.websocket.infra.handler;

import coffeeshout.global.websocket.event.session.SessionBaseEvent;
import coffeeshout.global.websocket.event.session.SessionEventType;

public interface SessionEventHandler<T extends SessionBaseEvent> {
    void handle(T event);
    SessionEventType getSupportedEventType();
}
