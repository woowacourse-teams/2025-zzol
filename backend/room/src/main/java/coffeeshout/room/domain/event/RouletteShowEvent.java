package coffeeshout.room.domain.event;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.trace.TraceInfo;
import coffeeshout.global.trace.TraceInfoExtractor;
import coffeeshout.global.trace.Traceable;
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
