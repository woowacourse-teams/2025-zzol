package coffeeshout.global.websocket.event.player;

import coffeeshout.global.trace.TraceInfo;
import java.time.LocalDateTime;

public interface PlayerBaseEvent {
    String eventId();

    TraceInfo traceInfo();

    LocalDateTime timestamp();

    PlayerEventType eventType();
}
