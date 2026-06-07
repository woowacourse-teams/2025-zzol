package coffeeshout.room.domain.event;

import coffeeshout.global.redis.BaseEvent;
import java.time.Instant;
import java.util.UUID;

public record RoomCreateEvent(
        String eventId,
        Instant timestamp,
        String hostName,
        String joinCode
) implements BaseEvent {

    public RoomCreateEvent(String hostName, String joinCode) {
        this(
                UUID.randomUUID().toString(),
                Instant.now(),
                hostName,
                joinCode
        );
    }
}
