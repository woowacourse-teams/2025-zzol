package coffeeshout.laddergame.domain.event;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.trace.TraceInfo;
import coffeeshout.global.trace.TraceInfoExtractor;
import coffeeshout.global.trace.Traceable;
import coffeeshout.laddergame.ui.request.LadderDrawRequest;
import java.time.Instant;
import java.util.UUID;

public record LadderDrawCommandEvent(
        String eventId,
        String joinCode,
        String playerName,
        int segmentIndex,
        Instant timestamp,
        TraceInfo traceInfo
) implements BaseEvent, Traceable {

    public static LadderDrawCommandEvent of(String joinCode, String playerName, LadderDrawRequest request) {
        return new LadderDrawCommandEvent(
                UUID.randomUUID().toString(),
                joinCode,
                playerName,
                request.segmentIndex(),
                Instant.now(),
                TraceInfoExtractor.extract()
        );
    }
}
