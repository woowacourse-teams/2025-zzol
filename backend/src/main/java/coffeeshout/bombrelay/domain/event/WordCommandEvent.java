package coffeeshout.bombrelay.domain.event;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.trace.TraceInfo;
import coffeeshout.global.trace.TraceInfoExtractor;
import coffeeshout.global.trace.Traceable;
import java.time.Instant;
import java.util.UUID;

public record WordCommandEvent(
        String eventId,
        String joinCode,
        String playerName,
        String word,
        Instant timestamp,
        TraceInfo traceInfo
) implements BaseEvent, Traceable {

    public static WordCommandEvent create(String joinCode, String playerName, String word) {
        return new WordCommandEvent(
                UUID.randomUUID().toString(),
                joinCode,
                playerName,
                word,
                Instant.now(),
                TraceInfoExtractor.extract()
        );
    }
}
