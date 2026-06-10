package coffeeshout.room.infra.messaging.consumer;

import coffeeshout.gamecommon.GameRoomCreatedEvent;
import coffeeshout.room.application.service.RoomService;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoomCreateConsumer implements Consumer<GameRoomCreatedEvent> {

    private final RoomService roomService;

    @Override
    public void accept(GameRoomCreatedEvent event) {
        roomService.createRoom(event);
    }
}
