package coffeeshout.room.infra.messaging.consumer;

import coffeeshout.room.application.service.RoomService;
import coffeeshout.room.domain.event.MiniGameSelectEvent;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MiniGameSelectConsumer implements Consumer<MiniGameSelectEvent> {

    private final RoomService roomService;

    @Override
    public void accept(MiniGameSelectEvent event) {
        roomService.updateMiniGames(event);
    }
}
