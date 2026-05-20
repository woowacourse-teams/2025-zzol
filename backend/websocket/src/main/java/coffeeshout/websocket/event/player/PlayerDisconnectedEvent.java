package coffeeshout.websocket.event.player;

import coffeeshout.redis.BaseEvent;
import coffeeshout.trace.TraceInfo;
import coffeeshout.trace.TraceInfoExtractor;
import coffeeshout.trace.Traceable;
import java.time.Instant;
import java.util.UUID;

public record PlayerDisconnectedEvent(
        String eventId,
        TraceInfo traceInfo,
        Instant timestamp,
        PlayerEventType eventType,
        String playerKey,
        String sessionId,
        String reason
) implements BaseEvent, Traceable {

    public static PlayerDisconnectedEvent create(String playerKey, String sessionId, String reason) {
        return new PlayerDisconnectedEvent(
                UUID.randomUUID().toString(),
                TraceInfoExtractor.extract(),
                Instant.now(),
                PlayerEventType.PLAYER_DISCONNECTED,
                playerKey,
                sessionId,
                reason
        );
    }
}
