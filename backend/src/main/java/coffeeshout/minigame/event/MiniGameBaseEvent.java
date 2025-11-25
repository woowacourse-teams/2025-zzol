package coffeeshout.minigame.event;

import coffeeshout.global.trace.TraceInfo;
import java.time.Instant;

public interface MiniGameBaseEvent {
    String eventId();

    TraceInfo traceInfo();

    Instant timestamp();

    MiniGameEventType eventType();
}
