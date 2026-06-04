package coffeeshout.minigame.event;

import java.time.Instant;

public interface MiniGameBaseEvent {
    String eventId();

    Instant timestamp();

    MiniGameEventType eventType();
}
