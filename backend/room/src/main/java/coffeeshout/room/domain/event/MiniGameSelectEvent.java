package coffeeshout.room.domain.event;

import coffeeshout.redis.BaseEvent;
import coffeeshout.trace.TraceInfo;
import coffeeshout.trace.TraceInfoExtractor;
import coffeeshout.trace.Traceable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MiniGameSelectEvent(
        String eventId,
        TraceInfo traceInfo,
        Instant timestamp,
        String joinCode,
        String hostName,
        List<String> miniGameTypeNames
) implements BaseEvent, Traceable {

    public MiniGameSelectEvent(String joinCode, String hostName, List<String> miniGameTypeNames) {
        this(
                UUID.randomUUID().toString(),
                TraceInfoExtractor.extract(),
                Instant.now(),
                joinCode,
                hostName,
                miniGameTypeNames
        );
    }

    @Override
    public TraceInfo traceInfo() {
        return traceInfo;
    }
}
