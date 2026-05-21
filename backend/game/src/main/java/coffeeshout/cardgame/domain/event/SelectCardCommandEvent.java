package coffeeshout.cardgame.domain.event;

import coffeeshout.redis.BaseEvent;
import coffeeshout.trace.TraceInfo;
import coffeeshout.trace.TraceInfoExtractor;
import coffeeshout.trace.Traceable;
import java.time.Instant;
import java.util.UUID;

public record SelectCardCommandEvent(
        String eventId,
        TraceInfo traceInfo,
        Instant timestamp,
        String joinCode,
        String playerName,
        Long userId,
        Integer cardIndex
) implements BaseEvent, Traceable {

    public SelectCardCommandEvent(String joinCode, String playerName, Long userId, Integer cardIndex) {
        this(
                UUID.randomUUID().toString(),
                TraceInfoExtractor.extract(),
                Instant.now(),
                joinCode,
                playerName,
                userId,
                cardIndex
        );
    }

    @Override
    public TraceInfo traceInfo() {
        return traceInfo;
    }
}
