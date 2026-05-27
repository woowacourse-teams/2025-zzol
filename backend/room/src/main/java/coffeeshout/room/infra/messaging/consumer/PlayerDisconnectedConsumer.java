package coffeeshout.room.infra.messaging.consumer;

import coffeeshout.room.infra.websocket.DelayedPlayerRemovalService;
import coffeeshout.websocket.event.player.PlayerDisconnectedEvent;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlayerDisconnectedConsumer implements Consumer<PlayerDisconnectedEvent> {

    private final DelayedPlayerRemovalService delayedPlayerRemovalService;

    @Override
    public void accept(PlayerDisconnectedEvent event) {
        delayedPlayerRemovalService.schedulePlayerRemoval(event.playerKey(), event.sessionId(), event.reason());
    }
}
