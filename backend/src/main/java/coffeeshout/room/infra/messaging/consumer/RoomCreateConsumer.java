package coffeeshout.room.infra.messaging.consumer;

import coffeeshout.room.application.service.RoomEventService;
import coffeeshout.room.domain.event.RoomCreateEvent;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoomCreateConsumer implements Consumer<RoomCreateEvent> {

    private final RoomEventService roomEventService;

    @Override
    public void accept(RoomCreateEvent event) {
        roomEventService.createRoom(event);
    }
}
