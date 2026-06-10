package coffeeshout.room.domain.event;

import coffeeshout.global.redis.BaseEvent;
import java.time.Instant;
import java.util.UUID;

public record RoomRemovedEvent(
        String eventId,
        Instant timestamp,
        String joinCode
) implements BaseEvent {

    public RoomRemovedEvent(String joinCode) {
        this(
                UUID.randomUUID().toString(),
                Instant.now(),
                joinCode
        );
    }
}
