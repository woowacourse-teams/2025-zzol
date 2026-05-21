package coffeeshout.blockstacking.domain.event;

import coffeeshout.redis.BaseEvent;
import coffeeshout.trace.TraceInfo;
import coffeeshout.trace.TraceInfoExtractor;
import coffeeshout.trace.Traceable;
import java.time.Instant;
import java.util.UUID;

public record BlockStackingFailEvent(
        String eventId,
        String joinCode,
        String playerName,
        Long userId,
        Instant timestamp,
        TraceInfo traceInfo
) implements BaseEvent, Traceable {

    public static BlockStackingFailEvent of(String joinCode, String playerName, Long userId) {
        return new BlockStackingFailEvent(
                UUID.randomUUID().toString(),
                joinCode,
                playerName,
                userId,
                Instant.now(),
                TraceInfoExtractor.extract()
        );
    }
}
