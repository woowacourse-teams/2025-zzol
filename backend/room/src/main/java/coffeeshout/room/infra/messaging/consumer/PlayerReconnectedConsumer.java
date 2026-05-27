package coffeeshout.room.infra.messaging.consumer;

import coffeeshout.room.infra.websocket.DelayedPlayerRemovalService;
import coffeeshout.websocket.event.player.PlayerReconnectedEvent;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlayerReconnectedConsumer implements Consumer<PlayerReconnectedEvent> {

    private final DelayedPlayerRemovalService delayedPlayerRemovalService;

    @Override
    public void accept(PlayerReconnectedEvent event) {
        delayedPlayerRemovalService.cancelScheduledRemoval(event.playerKey());
    }
}
