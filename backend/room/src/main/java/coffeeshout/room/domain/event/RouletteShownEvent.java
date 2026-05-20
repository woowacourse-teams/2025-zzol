package coffeeshout.room.domain.event;

import coffeeshout.redis.BaseEvent;
import coffeeshout.trace.TraceInfo;
import coffeeshout.trace.TraceInfoExtractor;
import coffeeshout.trace.Traceable;
import coffeeshout.room.domain.RoomState;
import java.time.Instant;
import java.util.UUID;

public record RouletteShownEvent(
        String eventId,
        TraceInfo traceInfo,
        Instant timestamp,
        String joinCode,
        RoomState roomState
) implements BaseEvent, Traceable {

    public RouletteShownEvent(String joinCode, RoomState roomState) {
        this(
                UUID.randomUUID().toString(),
                TraceInfoExtractor.extract(),
                Instant.now(),
                joinCode,
                roomState
        );
    }
}
