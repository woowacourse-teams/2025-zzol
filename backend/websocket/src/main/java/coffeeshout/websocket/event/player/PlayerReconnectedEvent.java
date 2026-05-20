package coffeeshout.websocket.event.player;

import coffeeshout.redis.BaseEvent;
import coffeeshout.trace.TraceInfo;
import coffeeshout.trace.TraceInfoExtractor;
import coffeeshout.trace.Traceable;
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
