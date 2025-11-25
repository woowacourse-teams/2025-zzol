package coffeeshout.room.domain.event;

import coffeeshout.global.trace.TraceInfo;
import coffeeshout.global.trace.TraceInfoExtractor;
import coffeeshout.global.trace.Traceable;
import coffeeshout.minigame.domain.MiniGameType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MiniGameSelectEvent(
        String eventId,
        TraceInfo traceInfo,
        Instant timestamp,
        RoomEventType eventType,
        String joinCode,
        String hostName,
        List<MiniGameType> miniGameTypes
) implements RoomBaseEvent, Traceable {

    public MiniGameSelectEvent(String joinCode, String hostName, List<MiniGameType> miniGameTypes) {
        this(
                UUID.randomUUID().toString(),
                TraceInfoExtractor.extract(),
                Instant.now(),
                RoomEventType.MINI_GAME_SELECT,
                joinCode,
                hostName,
                miniGameTypes
        );
    }

    @Override
    public TraceInfo getTraceInfo() {
        return traceInfo;
    }
}
