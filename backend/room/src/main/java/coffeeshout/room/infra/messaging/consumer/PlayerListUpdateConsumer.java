package coffeeshout.room.infra.messaging.consumer;

import coffeeshout.room.application.service.PlayerService;
import coffeeshout.room.domain.event.PlayerListUpdateEvent;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlayerListUpdateConsumer implements Consumer<PlayerListUpdateEvent> {

    private final PlayerService playerService;

    @Override
    public void accept(PlayerListUpdateEvent event) {
        playerService.updatePlayers(event);
    }
}
