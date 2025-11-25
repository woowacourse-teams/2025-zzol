package coffeeshout.global.websocket.event.player;

import coffeeshout.global.trace.TraceInfo;
import coffeeshout.global.trace.TraceInfoExtractor;
import coffeeshout.global.trace.Traceable;
import java.time.LocalDateTime;
import java.util.UUID;

public record PlayerReconnectedEvent(
        String eventId,
        TraceInfo traceInfo,
        LocalDateTime timestamp,
        PlayerEventType eventType,
        String playerKey,
        String sessionId
) implements PlayerBaseEvent, Traceable {

    public static PlayerReconnectedEvent create(String playerKey, String sessionId) {
        return new PlayerReconnectedEvent(
                UUID.randomUUID().toString(),
                TraceInfoExtractor.extract(),
                LocalDateTime.now(),
                PlayerEventType.PLAYER_RECONNECTED,
                playerKey,
                sessionId
        );
    }

    @Override
    public TraceInfo getTraceInfo() {
        return traceInfo;
    }
}
