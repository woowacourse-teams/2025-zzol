package coffeeshout.room.domain.event;

import coffeeshout.global.trace.TraceInfo;
import coffeeshout.global.trace.TraceInfoExtractor;
import coffeeshout.global.trace.Traceable;
import coffeeshout.room.ui.request.SelectedMenuRequest;
import java.time.Instant;
import java.util.UUID;

public record RoomCreateEvent(
        String eventId,
        TraceInfo traceInfo,
        Instant timestamp,
        RoomEventType eventType,
        String hostName,
        SelectedMenuRequest selectedMenuRequest,
        String joinCode
) implements RoomBaseEvent, Traceable {

    public RoomCreateEvent(String hostName,
                           SelectedMenuRequest selectedMenuRequest,
                           String joinCode) {
        this(
                UUID.randomUUID().toString(),
                TraceInfoExtractor.extract(),
                Instant.now(),
                RoomEventType.ROOM_CREATE,
                hostName,
                selectedMenuRequest,
                joinCode
        );
    }

    @Override
    public TraceInfo getTraceInfo() {
        return traceInfo;
    }
}
