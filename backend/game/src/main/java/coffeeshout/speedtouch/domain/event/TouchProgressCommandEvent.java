package coffeeshout.speedtouch.domain.event;

import coffeeshout.redis.BaseEvent;
import coffeeshout.trace.TraceInfo;
import coffeeshout.trace.TraceInfoExtractor;
import coffeeshout.trace.Traceable;
import java.time.Instant;
import java.util.UUID;

public record TouchProgressCommandEvent(
        String eventId,
        String joinCode,
        String playerName,
        int touchedNumber,
        Instant timestamp,
        TraceInfo traceInfo
) implements BaseEvent, Traceable {

    public static TouchProgressCommandEvent create(String joinCode, String playerName, int touchedNumber) {
        return new TouchProgressCommandEvent(
                UUID.randomUUID().toString(),
                joinCode,
                playerName,
                touchedNumber,
                Instant.now(),
                TraceInfoExtractor.extract()
        );
    }
}
