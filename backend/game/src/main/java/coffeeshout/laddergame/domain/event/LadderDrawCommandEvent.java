package coffeeshout.laddergame.domain.event;

import coffeeshout.redis.BaseEvent;
import coffeeshout.trace.TraceInfo;
import coffeeshout.trace.TraceInfoExtractor;
import coffeeshout.trace.Traceable;
import java.time.Instant;
import java.util.UUID;

public record LadderDrawCommandEvent(
        String eventId,
        String joinCode,
        String playerName,
        Long userId,
        int segmentIndex,
        Instant timestamp,
        TraceInfo traceInfo
) implements BaseEvent, Traceable {

    public static LadderDrawCommandEvent of(String joinCode, String playerName, Long userId, int segmentIndex) {
        return new LadderDrawCommandEvent(
                UUID.randomUUID().toString(),
                joinCode,
                playerName,
                userId,
                segmentIndex,
                Instant.now(),
                TraceInfoExtractor.extract()
        );
    }
}
