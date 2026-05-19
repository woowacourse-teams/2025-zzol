package coffeeshout.websocket.event.session;

import java.time.LocalDateTime;

public interface SessionBaseEvent {
    String eventId();
    SessionEventType eventType();
    LocalDateTime timestamp();
}
