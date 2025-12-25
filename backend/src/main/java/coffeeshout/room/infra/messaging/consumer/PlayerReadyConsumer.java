package coffeeshout.room.infra.messaging.consumer;

import coffeeshout.room.application.service.RoomEventService;
import coffeeshout.room.domain.event.PlayerReadyEvent;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlayerReadyConsumer implements Consumer<PlayerReadyEvent> {

    private final RoomEventService roomEventService;

    @Override
    public void accept(PlayerReadyEvent event) {
        roomEventService.readyPlayer(event);
    }
}
