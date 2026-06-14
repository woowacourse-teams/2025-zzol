package coffeeshout.websocket.event.player;

import coffeeshout.global.redis.BaseEvent;
import java.time.Instant;
import java.util.UUID;

public record PlayerDisconnectedEvent(
        String eventId,
        Instant timestamp,
        PlayerEventType eventType,
        String playerKey,
        String sessionId,
        String reason
) implements BaseEvent {

    public static PlayerDisconnectedEvent create(String playerKey, String sessionId, String reason) {
        return new PlayerDisconnectedEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                PlayerEventType.PLAYER_DISCONNECTED,
                playerKey,
                sessionId,
                reason
        );
    }
}
