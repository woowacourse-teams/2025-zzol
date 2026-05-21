package coffeeshout.room.domain.event;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.trace.TraceInfo;
import coffeeshout.global.trace.TraceInfoExtractor;
import coffeeshout.global.trace.Traceable;
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
