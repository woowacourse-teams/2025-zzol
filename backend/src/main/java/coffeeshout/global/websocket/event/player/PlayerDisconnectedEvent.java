package coffeeshout.global.websocket.event.player;

import coffeeshout.global.trace.TraceInfo;
import coffeeshout.global.trace.TraceInfoExtractor;
import coffeeshout.global.trace.Traceable;
import java.time.LocalDateTime;
import java.util.UUID;

public record PlayerDisconnectedEvent(
        String eventId,
        TraceInfo traceInfo,
        LocalDateTime timestamp,
        PlayerEventType eventType,
        String playerKey,
        String sessionId,
        String reason
) implements PlayerBaseEvent, Traceable {

    public static PlayerDisconnectedEvent create(String playerKey, String sessionId, String reason) {
        return new PlayerDisconnectedEvent(
                UUID.randomUUID().toString(),
                TraceInfoExtractor.extract(),
                LocalDateTime.now(),
                PlayerEventType.PLAYER_DISCONNECTED,
                playerKey,
                sessionId,
                reason
        );
    }

    @Override
    public TraceInfo getTraceInfo() {
        return traceInfo;
    }
}
