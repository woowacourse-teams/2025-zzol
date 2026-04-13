package coffeeshout.blockstacking.domain.event;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.trace.TraceInfo;
import coffeeshout.global.trace.TraceInfoExtractor;
import coffeeshout.global.trace.Traceable;
import java.time.Instant;
import java.util.UUID;

public record BlockStackingFailEvent(
        String eventId,
        String joinCode,
        String playerName,
        Instant timestamp,
        TraceInfo traceInfo
) implements BaseEvent, Traceable {

    public static BlockStackingFailEvent of(String joinCode, String playerName) {
        return new BlockStackingFailEvent(
                UUID.randomUUID().toString(),
                joinCode,
                playerName,
                Instant.now(),
                TraceInfoExtractor.extract()
        );
    }
}
