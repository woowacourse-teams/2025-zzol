package coffeeshout.room.domain.event;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.trace.TraceInfo;
import coffeeshout.global.trace.TraceInfoExtractor;
import coffeeshout.global.trace.Traceable;
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
