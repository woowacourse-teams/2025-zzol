package coffeeshout.room.domain.event;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.trace.TraceInfo;
import coffeeshout.global.trace.TraceInfoExtractor;
import coffeeshout.global.trace.Traceable;
import java.time.Instant;
import java.util.UUID;

public record PlayerReadyEvent(
        String eventId,
        TraceInfo traceInfo,
        Instant timestamp,
        String joinCode,
        String playerName,
        Boolean isReady
) implements BaseEvent, Traceable {

    public PlayerReadyEvent(String joinCode, String playerName, Boolean isReady) {
        this(
                UUID.randomUUID().toString(),
                TraceInfoExtractor.extract(),
                Instant.now(),
                joinCode,
                playerName,
                isReady
        );
    }

    @Override
    public TraceInfo traceInfo() {
        return traceInfo;
    }
}
