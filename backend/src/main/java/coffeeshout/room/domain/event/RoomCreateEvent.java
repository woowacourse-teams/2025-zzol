package coffeeshout.room.domain.event;

import coffeeshout.global.trace.TraceInfo;
import coffeeshout.global.trace.TraceInfoExtractor;
import coffeeshout.global.trace.Traceable;
import java.time.Instant;
import java.util.UUID;

public record RoomCreateEvent(
        String eventId,
        TraceInfo traceInfo,
        Instant timestamp,
        RoomEventType eventType,
        String hostName,
        String joinCode
) implements RoomBaseEvent, Traceable {

    public RoomCreateEvent(String hostName,
                           String joinCode) {
        this(
                UUID.randomUUID().toString(),
                TraceInfoExtractor.extract(),
                Instant.now(),
                RoomEventType.ROOM_CREATE,
                hostName,
                joinCode
        );
    }

    @Override
    public TraceInfo getTraceInfo() {
        return traceInfo;
    }
}
