package coffeeshout.room.domain.event;

import coffeeshout.global.redis.BaseEvent;
import java.time.Instant;
import java.util.UUID;

public record RouletteShowEvent(
        String eventId,
        Instant timestamp,
        String joinCode
) implements BaseEvent {

    public RouletteShowEvent(String joinCode) {
        this(
                UUID.randomUUID().toString(),
                Instant.now(),
                joinCode
        );
    }
}
