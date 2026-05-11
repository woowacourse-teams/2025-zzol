package coffeeshout.room.domain.event;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.trace.TraceInfo;
import coffeeshout.global.trace.TraceInfoExtractor;
import coffeeshout.global.trace.Traceable;
import java.time.Instant;
import java.util.UUID;

public record RoomJoinEvent(
        String eventId,
        TraceInfo traceInfo,
        Instant timestamp,
        String joinCode,
        String guestName,
        Long userId
) implements BaseEvent, Traceable {

    public RoomJoinEvent(String joinCode, String guestName) {
        this(joinCode, guestName, null);
    }

    public RoomJoinEvent(String joinCode, String guestName, Long userId) {
        this(
                UUID.randomUUID().toString(),
                TraceInfoExtractor.extract(),
                Instant.now(),
                joinCode,
                guestName,
                userId
        );
    }
}
