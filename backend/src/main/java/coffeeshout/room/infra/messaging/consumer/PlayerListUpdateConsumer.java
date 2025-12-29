package coffeeshout.room.infra.messaging.consumer;

import coffeeshout.room.application.service.RoomEventService;
import coffeeshout.room.domain.event.PlayerListUpdateEvent;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlayerListUpdateConsumer implements Consumer<PlayerListUpdateEvent> {

    private final RoomEventService roomEventService;

    @Override
    public void accept(PlayerListUpdateEvent event) {
        roomEventService.updatePlayers(event);
    }
}
