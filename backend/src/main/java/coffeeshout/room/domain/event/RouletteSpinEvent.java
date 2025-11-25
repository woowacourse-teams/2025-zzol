package coffeeshout.room.domain.event;

import coffeeshout.global.trace.TraceInfo;
import coffeeshout.global.trace.TraceInfoExtractor;
import coffeeshout.global.trace.Traceable;
import coffeeshout.room.domain.player.Winner;
import java.time.Instant;
import java.util.UUID;

public record RouletteSpinEvent(
        String eventId,
        TraceInfo traceInfo,
        Instant timestamp,
        RoomEventType eventType,
        String joinCode,
        String hostName,
        Winner winner
) implements RoomBaseEvent, Traceable {

    public RouletteSpinEvent(String joinCode, String hostName, Winner winner) {
        this(
                UUID.randomUUID().toString(),
                TraceInfoExtractor.extract(),
                Instant.now(),
                RoomEventType.ROULETTE_SPIN,
                joinCode,
                hostName,
                winner
        );
    }

    @Override
    public TraceInfo getTraceInfo() {
        return traceInfo;
    }
}
