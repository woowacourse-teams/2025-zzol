package coffeeshout.room.infra.messaging.consumer;

import coffeeshout.room.application.service.RoomEventService;
import coffeeshout.room.domain.event.PlayerKickEvent;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlayerKickConsumer implements Consumer<PlayerKickEvent> {

    private final RoomEventService roomEventService;

    @Override
    public void accept(PlayerKickEvent event) {
        roomEventService.kickPlayer(event);
    }
}
