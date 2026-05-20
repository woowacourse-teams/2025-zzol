package coffeeshout.blockstacking.domain.event;

import coffeeshout.blockstacking.ui.request.BlockStackingProgressRequest;
import coffeeshout.redis.BaseEvent;
import coffeeshout.trace.TraceInfo;
import coffeeshout.trace.TraceInfoExtractor;
import coffeeshout.trace.Traceable;
import java.time.Instant;
import java.util.UUID;

public record BlockStackingCommandEvent(
        String eventId,
        String joinCode,
        String playerName,
        int floor,
        double movingBlockX,
        double stackTopX,
        double stackTopWidth,
        Instant timestamp,
        TraceInfo traceInfo
) implements BaseEvent, Traceable {

    public static BlockStackingCommandEvent of(
            String joinCode, String authenticatedPlayerName, BlockStackingProgressRequest request
    ) {
        return new BlockStackingCommandEvent(
                UUID.randomUUID().toString(),
                joinCode,
                authenticatedPlayerName,
                request.floor(),
                request.movingBlockX(),
                request.stackTopX(),
                request.stackTopWidth(),
                Instant.now(),
                TraceInfoExtractor.extract()
        );
    }
}
