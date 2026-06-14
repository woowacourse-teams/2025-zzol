package coffeeshout.room.domain.event;

import coffeeshout.global.redis.BaseEvent;
import java.time.Instant;
import java.util.UUID;

public record PlayerReadyEvent(
        String eventId,
        Instant timestamp,
        String joinCode,
        String playerName,
        Boolean isReady
) implements BaseEvent {

    public PlayerReadyEvent(String joinCode, String playerName, Boolean isReady) {
        this(
                UUID.randomUUID().toString(),
                Instant.now(),
                joinCode,
                playerName,
                isReady
        );
    }
}
