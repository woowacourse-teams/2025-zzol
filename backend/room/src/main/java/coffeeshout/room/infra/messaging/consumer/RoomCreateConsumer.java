package coffeeshout.room.infra.messaging.consumer;

import coffeeshout.room.application.service.RoomService;
import coffeeshout.room.domain.event.RoomCreateEvent;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoomCreateConsumer implements Consumer<RoomCreateEvent> {

    private final RoomService roomService;

    @Override
    public void accept(RoomCreateEvent event) {
        roomService.createRoom(event);
    }
}
