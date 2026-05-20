package coffeeshout.room.domain.event;

import coffeeshout.redis.BaseEvent;
import coffeeshout.trace.TraceInfo;
import coffeeshout.trace.TraceInfoExtractor;
import coffeeshout.trace.Traceable;
import coffeeshout.room.domain.player.Winner;
import java.time.Instant;
import java.util.UUID;

public record RouletteSpinEvent(
        String eventId,
        TraceInfo traceInfo,
        Instant timestamp,
        String joinCode,
        String hostName,
        Winner winner
) implements BaseEvent, Traceable {

    public RouletteSpinEvent(String joinCode, String hostName, Winner winner) {
        this(
                UUID.randomUUID().toString(),
                TraceInfoExtractor.extract(),
                Instant.now(),
                joinCode,
                hostName,
                winner
        );
    }

    @Override
    public TraceInfo traceInfo() {
        return traceInfo;
    }
}
