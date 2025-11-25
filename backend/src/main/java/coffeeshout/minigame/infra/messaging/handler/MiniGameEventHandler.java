package coffeeshout.minigame.infra.messaging.handler;

import coffeeshout.minigame.event.MiniGameBaseEvent;
import coffeeshout.minigame.event.MiniGameEventType;

public interface MiniGameEventHandler<T extends MiniGameBaseEvent> {
    void handle(T event);
    MiniGameEventType getSupportedEventType();
}
