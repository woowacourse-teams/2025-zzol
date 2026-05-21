package coffeeshout.room.domain.event;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.trace.TraceInfo;
import coffeeshout.global.trace.TraceInfoExtractor;
import coffeeshout.global.trace.Traceable;
import java.time.Instant;
import java.util.UUID;

/*
 * 해당 이벤트를 Spring ApplicationEventPublisher를 통해 발행하면, WebSocket을 통해 Player 정보를 클라이언트에게 전달됩니다.
 */

public record PlayerListUpdateEvent(
        String eventId,
        TraceInfo traceInfo,
        Instant timestamp,
        String joinCode
) implements BaseEvent, Traceable {

    public PlayerListUpdateEvent(String joinCode) {
        this(
                UUID.randomUUID().toString(),
                TraceInfoExtractor.extract(),
                Instant.now(),
                joinCode
        );
    }
}
