package coffeeshout.room.domain.event;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.trace.TraceInfo;
import coffeeshout.global.trace.TraceInfoExtractor;
import coffeeshout.global.trace.Traceable;
import coffeeshout.room.ui.request.SelectedMenuRequest;
import java.time.Instant;
import java.util.UUID;

public record RoomJoinEvent(
        String eventId,
        TraceInfo traceInfo,
        Instant timestamp,
        String joinCode,
        String guestName,
        SelectedMenuRequest selectedMenuRequest
) implements BaseEvent, Traceable {

    public RoomJoinEvent(String joinCode, String guestName, SelectedMenuRequest selectedMenuRequest) {
        this(
                UUID.randomUUID().toString(),
                TraceInfoExtractor.extract(),
                Instant.now(),
                joinCode,
                guestName,
                selectedMenuRequest
        );
    }
}
