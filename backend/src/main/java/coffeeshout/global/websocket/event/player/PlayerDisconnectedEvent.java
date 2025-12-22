package coffeeshout.global.websocket.event.player;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.trace.TraceInfo;
import coffeeshout.global.trace.TraceInfoExtractor;
import coffeeshout.global.trace.Traceable;
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

    @Override
    public TraceInfo traceInfo() {
        return traceInfo;
    }
}
