package coffeeshout.minigame.event;

import coffeeshout.global.redis.BaseEvent;
import java.time.Instant;
import java.util.UUID;

public record StartMiniGameCommandEvent(
        String eventId,
        Instant timestamp,
        String joinCode,
        String hostName
) implements BaseEvent {

    public StartMiniGameCommandEvent(String joinCode, String hostName) {
        this(
                UUID.randomUUID().toString(),
                Instant.now(),
                joinCode,
                hostName
        );
    }
}
