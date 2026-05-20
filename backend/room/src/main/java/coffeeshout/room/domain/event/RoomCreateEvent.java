package coffeeshout.room.domain.event;

import coffeeshout.redis.BaseEvent;
import coffeeshout.trace.TraceInfo;
import coffeeshout.trace.TraceInfoExtractor;
import coffeeshout.trace.Traceable;
import java.time.Instant;
import java.util.UUID;

public record RoomCreateEvent(
        String eventId,
        TraceInfo traceInfo,
        Instant timestamp,
        String hostName,
        String joinCode
) implements BaseEvent, Traceable {

    public RoomCreateEvent(String hostName, String joinCode) {
        this(
                UUID.randomUUID().toString(),
                TraceInfoExtractor.extract(),
                Instant.now(),
                hostName,
                joinCode
        );
    }

    @Override
    public TraceInfo traceInfo() {
        return traceInfo;
    }
}
