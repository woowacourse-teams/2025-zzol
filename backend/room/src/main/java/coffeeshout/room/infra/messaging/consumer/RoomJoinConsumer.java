package coffeeshout.room.infra.messaging.consumer;

import coffeeshout.room.application.service.RoomService;
import coffeeshout.room.domain.event.RoomJoinEvent;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoomJoinConsumer implements Consumer<RoomJoinEvent> {

    private final RoomService roomService;

    @Override
    public void accept(RoomJoinEvent event) {
        roomService.joinRoom(event);
    }
}
