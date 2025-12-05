package coffeeshout.room.domain.event;

import coffeeshout.global.trace.TraceInfo;
import coffeeshout.global.trace.TraceInfoExtractor;
import coffeeshout.global.trace.Traceable;
import java.time.Instant;
import java.util.UUID;

public record RoomJoinEvent(
        String eventId,
        TraceInfo traceInfo,
        Instant timestamp,
        RoomEventType eventType,
        String joinCode,
        String guestName
) implements RoomBaseEvent, Traceable {

    public RoomJoinEvent(String joinCode, String guestName) {
        this(
                UUID.randomUUID().toString(),
                TraceInfoExtractor.extract(),
                Instant.now(),
                RoomEventType.ROOM_JOIN,
                joinCode,
                guestName
        );
    }

    @Override
    public TraceInfo getTraceInfo() {
        return traceInfo;
    }
}
