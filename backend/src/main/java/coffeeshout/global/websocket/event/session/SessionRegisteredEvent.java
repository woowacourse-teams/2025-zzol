package coffeeshout.global.websocket.event.session;

import java.time.LocalDateTime;
import java.util.UUID;

public record SessionRegisteredEvent(
        String eventId,
        SessionEventType eventType,
        String playerKey,
        String sessionId,
        LocalDateTime timestamp
) implements SessionBaseEvent {

    public static SessionRegisteredEvent create(String playerKey, String sessionId) {
        return new SessionRegisteredEvent(
                UUID.randomUUID().toString(),
                SessionEventType.SESSION_REGISTERED,
                playerKey,
                sessionId,
                LocalDateTime.now()
        );
    }
}
