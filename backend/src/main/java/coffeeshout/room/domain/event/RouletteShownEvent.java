package coffeeshout.room.domain.event;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.room.domain.RoomState;
import java.time.Instant;
import java.util.UUID;

public record RouletteShownEvent(
        String eventId,
        Instant timestamp,
        String joinCode,
        RoomState roomState
) implements BaseEvent {

    public RouletteShownEvent(String joinCode, RoomState roomState) {
        this(
                UUID.randomUUID().toString(),
                Instant.now(),
                joinCode,
                roomState
        );
    }
}
