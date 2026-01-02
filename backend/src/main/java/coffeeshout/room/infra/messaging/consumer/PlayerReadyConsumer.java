package coffeeshout.room.infra.messaging.consumer;

import coffeeshout.room.application.service.RoomService;
import coffeeshout.room.domain.event.PlayerReadyEvent;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlayerReadyConsumer implements Consumer<PlayerReadyEvent> {

    private final RoomService roomService;

    @Override
    public void accept(PlayerReadyEvent event) {
        roomService.readyPlayer(event);
    }
}
