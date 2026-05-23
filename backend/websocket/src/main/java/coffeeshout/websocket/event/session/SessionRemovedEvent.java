package coffeeshout.websocket.event.session;

import coffeeshout.global.redis.BaseEvent;
import java.time.Instant;
import java.util.UUID;

public record SessionRemovedEvent(
        String eventId,
        SessionEventType eventType,
        String sessionId,
        Instant timestamp
) implements BaseEvent {

    public static SessionRemovedEvent create(String sessionId) {
        return new SessionRemovedEvent(
                UUID.randomUUID().toString(),
                SessionEventType.SESSION_REMOVED,
                sessionId,
                Instant.now()
        );
    }
}
