package coffeeshout.global.websocket.event.player;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.trace.TraceInfo;
import coffeeshout.global.trace.TraceInfoExtractor;
import coffeeshout.global.trace.Traceable;
import java.time.Instant;
import java.util.UUID;

public record PlayerReconnectedEvent(
        String eventId,
        TraceInfo traceInfo,
        Instant timestamp,
        PlayerEventType eventType,
        String playerKey,
        String sessionId
) implements BaseEvent, Traceable {

    public static PlayerReconnectedEvent create(String playerKey, String sessionId) {
        return new PlayerReconnectedEvent(
                UUID.randomUUID().toString(),
                TraceInfoExtractor.extract(),
                Instant.now(),
                PlayerEventType.PLAYER_RECONNECTED,
                playerKey,
                sessionId
        );
    }

    @Override
    public TraceInfo traceInfo() {
        return traceInfo;
    }
}
