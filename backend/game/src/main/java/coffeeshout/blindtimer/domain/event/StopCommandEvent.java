package coffeeshout.blindtimer.domain.event;

import coffeeshout.redis.BaseEvent;
import coffeeshout.trace.TraceInfo;
import coffeeshout.trace.TraceInfoExtractor;
import coffeeshout.trace.Traceable;
import java.time.Instant;
import java.util.UUID;

public record StopCommandEvent(
        String eventId,
        String joinCode,
        String playerName,
        Long userId,
        Instant timestamp,
        TraceInfo traceInfo
) implements BaseEvent, Traceable {

    public static StopCommandEvent create(String joinCode, String playerName, Long userId) {
        return new StopCommandEvent(
                UUID.randomUUID().toString(),
                joinCode,
                playerName,
                userId,
                Instant.now(),
                TraceInfoExtractor.extract()
        );
    }
}
