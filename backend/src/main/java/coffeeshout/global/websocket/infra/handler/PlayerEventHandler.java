package coffeeshout.global.websocket.infra.handler;

import coffeeshout.global.websocket.event.player.PlayerBaseEvent;
import coffeeshout.global.websocket.event.player.PlayerEventType;

public interface PlayerEventHandler<T extends PlayerBaseEvent> {
    void handle(T event);
    
    PlayerEventType getSupportedEventType();
}
