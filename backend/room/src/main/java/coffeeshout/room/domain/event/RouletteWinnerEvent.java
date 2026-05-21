package coffeeshout.room.domain.event;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.trace.TraceInfo;
import coffeeshout.global.trace.TraceInfoExtractor;
import coffeeshout.global.trace.Traceable;
import coffeeshout.room.domain.player.Winner;
import java.time.Instant;
import java.util.UUID;

public record RouletteWinnerEvent(
        String eventId,
        TraceInfo traceInfo,
        Instant timestamp,
        String joinCode,
        Winner winner
) implements BaseEvent, Traceable {

    public RouletteWinnerEvent(String joinCode, Winner winner) {
        this(
                UUID.randomUUID().toString(),
                TraceInfoExtractor.extract(),
                Instant.now(),
                joinCode,
                winner
        );
    }
}
