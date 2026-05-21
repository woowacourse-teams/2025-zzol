package coffeeshout.racinggame.domain.event;

import coffeeshout.redis.BaseEvent;
import coffeeshout.trace.TraceInfo;
import coffeeshout.trace.TraceInfoExtractor;
import coffeeshout.trace.Traceable;
import java.time.Instant;
import java.util.UUID;

public record TapCommandEvent(
        String eventId,
        String joinCode,
        String playerName,
        Long userId,
        int tapCount,
        Instant timestamp,
        TraceInfo traceInfo
) implements BaseEvent, Traceable {

    public static TapCommandEvent create(String joinCode, String playerName, Long userId, int tapCount) {
        final String eventId = UUID.randomUUID().toString();
        return new TapCommandEvent(
                eventId,
                joinCode,
                playerName,
                userId,
                tapCount,
                Instant.now(),
                TraceInfoExtractor.extract()
        );
    }
}
