package coffeeshout.room.domain.event;

import coffeeshout.global.trace.TraceInfo;
import coffeeshout.global.trace.TraceInfoExtractor;
import coffeeshout.global.trace.Traceable;
import java.time.Instant;
import java.util.UUID;

public record RouletteShowEvent(
        String eventId,
        TraceInfo traceInfo,
        Instant timestamp,
        RoomEventType eventType,
        String joinCode
) implements RoomBaseEvent, Traceable {

    public RouletteShowEvent(String joinCode) {
        this(
                UUID.randomUUID().toString(),
                TraceInfoExtractor.extract(),
                Instant.now(),
                RoomEventType.ROULETTE_SHOW,
                joinCode
        );
    }

    @Override
    public TraceInfo getTraceInfo() {
        return traceInfo;
    }
}
