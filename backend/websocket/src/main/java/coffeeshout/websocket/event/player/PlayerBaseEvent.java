package coffeeshout.websocket.event.player;

import coffeeshout.trace.TraceInfo;
import java.time.LocalDateTime;

public interface PlayerBaseEvent {
    String eventId();

    TraceInfo traceInfo();

    LocalDateTime timestamp();

    PlayerEventType eventType();
}
