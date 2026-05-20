package coffeeshout.room.domain.event;

import coffeeshout.redis.BaseEvent;
import coffeeshout.trace.TraceInfo;
import coffeeshout.trace.TraceInfoExtractor;
import coffeeshout.trace.Traceable;
import coffeeshout.minigame.domain.MiniGameType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MiniGameSelectEvent(
        String eventId,
        TraceInfo traceInfo,
        Instant timestamp,
        String joinCode,
        String hostName,
        List<MiniGameType> miniGameTypes
) implements BaseEvent, Traceable {

    public MiniGameSelectEvent(String joinCode, String hostName, List<MiniGameType> miniGameTypes) {
        this(
                UUID.randomUUID().toString(),
                TraceInfoExtractor.extract(),
                Instant.now(),
                joinCode,
                hostName,
                miniGameTypes
        );
    }

    @Override
    public TraceInfo traceInfo() {
        return traceInfo;
    }
}
