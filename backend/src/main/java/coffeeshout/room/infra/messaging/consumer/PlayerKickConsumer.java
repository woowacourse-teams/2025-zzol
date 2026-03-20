package coffeeshout.room.infra.messaging.consumer;

import coffeeshout.room.application.service.PlayerService;
import coffeeshout.room.domain.event.PlayerKickEvent;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlayerKickConsumer implements Consumer<PlayerKickEvent> {

    private final PlayerService playerService;

    @Override
    public void accept(PlayerKickEvent event) {
        playerService.kickPlayer(event);
    }
}
