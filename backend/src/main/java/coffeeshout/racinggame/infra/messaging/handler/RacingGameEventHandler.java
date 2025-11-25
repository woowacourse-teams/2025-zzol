package coffeeshout.racinggame.infra.messaging.handler;

import coffeeshout.racinggame.domain.event.RacingGameEventType;

public interface RacingGameEventHandler<T> {

    void handle(T event);

    RacingGameEventType getSupportedEventType();
}
