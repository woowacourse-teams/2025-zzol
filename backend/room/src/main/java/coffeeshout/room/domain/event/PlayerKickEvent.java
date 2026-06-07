package coffeeshout.room.domain.event;

import coffeeshout.global.redis.BaseEvent;
import java.time.Instant;
import java.util.UUID;

public record PlayerKickEvent(
        String eventId,
        Instant timestamp,
        String joinCode,
        String playerName
) implements BaseEvent {

    public PlayerKickEvent(String joinCode, String playerName) {
        this(
                UUID.randomUUID().toString(),
                Instant.now(),
                joinCode,
                playerName
        );
    }
}
