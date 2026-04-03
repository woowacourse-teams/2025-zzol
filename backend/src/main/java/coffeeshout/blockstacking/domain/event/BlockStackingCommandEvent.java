package coffeeshout.blockstacking.domain.event;

import coffeeshout.blockstacking.ui.request.BlockStackingProgressRequest;
import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.trace.TraceInfo;
import coffeeshout.global.trace.TraceInfoExtractor;
import coffeeshout.global.trace.Traceable;
import java.time.Instant;
import java.util.UUID;

public record BlockStackingCommandEvent(
        String eventId,
        String joinCode,
        String playerName,
        int floor,
        double tapX,
        double movingBlockX,
        double stackTopX,
        double stackTopWidth,
        Instant timestamp,
        TraceInfo traceInfo
) implements BaseEvent, Traceable {

    public static BlockStackingCommandEvent create(String joinCode, String playerName, int floor, double tapX,
                                                   double movingBlockX, double stackTopX, double stackTopWidth) {
        return new BlockStackingCommandEvent(
                UUID.randomUUID().toString(),
                joinCode,
                playerName,
                floor,
                tapX,
                movingBlockX,
                stackTopX,
                stackTopWidth,
                Instant.now(),
                TraceInfoExtractor.extract()
        );
    }

    public static BlockStackingCommandEvent of(String joinCode, BlockStackingProgressRequest request) {
        return new BlockStackingCommandEvent(
                UUID.randomUUID().toString(),
                joinCode,
                request.playerName(),
                request.floor(),
                request.tapX(),
                request.movingBlockX(),
                request.stackTopX(),
                request.stackTopWidth(),
                Instant.now(),
                TraceInfoExtractor.extract()
        );
    }
}
