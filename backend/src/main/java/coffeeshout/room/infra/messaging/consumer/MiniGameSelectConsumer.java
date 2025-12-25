package coffeeshout.room.infra.messaging.consumer;

import coffeeshout.room.application.service.RoomEventService;
import coffeeshout.room.domain.event.MiniGameSelectEvent;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MiniGameSelectConsumer implements Consumer<MiniGameSelectEvent> {

    private final RoomEventService roomEventService;

    @Override
    public void accept(MiniGameSelectEvent event) {
        roomEventService.updateMiniGames(event);
    }
}
