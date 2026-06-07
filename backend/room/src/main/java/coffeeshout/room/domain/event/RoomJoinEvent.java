package coffeeshout.room.domain.event;

import coffeeshout.global.redis.BaseEvent;
import java.time.Instant;
import java.util.UUID;

public record RoomJoinEvent(
        String eventId,
        Instant timestamp,
        String joinCode,
        String guestName,
        Long userId
) implements BaseEvent {

    public RoomJoinEvent(String joinCode, String guestName) {
        this(joinCode, guestName, null);
    }

    public RoomJoinEvent(String joinCode, String guestName, Long userId) {
        this(
                UUID.randomUUID().toString(),
                Instant.now(),
                joinCode,
                guestName,
                userId
        );
    }
}
