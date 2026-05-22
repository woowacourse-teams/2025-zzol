package coffeeshout.websocket.event.player;

import coffeeshout.global.redis.BaseEvent;
import java.time.Instant;
import java.util.UUID;

public record PlayerReconnectedEvent(
        String eventId,
        Instant timestamp,
        PlayerEventType eventType,
        String playerKey,
        String sessionId
) implements BaseEvent {

    public static PlayerReconnectedEvent create(String playerKey, String sessionId) {
        return new PlayerReconnectedEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                PlayerEventType.PLAYER_RECONNECTED,
                playerKey,
                sessionId
        );
    }
}
