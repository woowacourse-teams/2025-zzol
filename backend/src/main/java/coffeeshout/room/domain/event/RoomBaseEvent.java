package coffeeshout.room.domain.event;

import java.time.Instant;

public interface RoomBaseEvent {

    String eventId();

    Instant timestamp();

    RoomEventType eventType();
}
