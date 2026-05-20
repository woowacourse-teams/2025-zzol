package coffeeshout.room.domain.event;

import coffeeshout.redis.BaseEvent;
import coffeeshout.trace.TraceInfo;
import coffeeshout.trace.TraceInfoExtractor;
import coffeeshout.trace.Traceable;
import java.time.Instant;
import java.util.UUID;

public record RouletteShowEvent(
        String eventId,
        TraceInfo traceInfo,
        Instant timestamp,
        String joinCode
) implements BaseEvent, Traceable {

    public RouletteShowEvent(String joinCode) {
        this(
                UUID.randomUUID().toString(),
                TraceInfoExtractor.extract(),
                Instant.now(),
                joinCode
        );
    }

    @Override
    public TraceInfo traceInfo() {
        return traceInfo;
    }
}
