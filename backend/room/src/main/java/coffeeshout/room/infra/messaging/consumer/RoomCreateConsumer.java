package coffeeshout.room.infra.messaging.consumer;

import coffeeshout.gamecommon.RoomLifecycleEvent;
import coffeeshout.room.application.service.RoomService;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoomCreateConsumer implements Consumer<RoomLifecycleEvent.Created> {

    private final RoomService roomService;

    @Override
    public void accept(RoomLifecycleEvent.Created event) {
        roomService.createRoom(event);
    }
}
