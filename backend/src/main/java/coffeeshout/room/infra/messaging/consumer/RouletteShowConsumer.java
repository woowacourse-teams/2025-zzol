package coffeeshout.room.infra.messaging.consumer;

import coffeeshout.room.application.service.RoomEventService;
import coffeeshout.room.domain.event.RouletteShowEvent;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RouletteShowConsumer implements Consumer<RouletteShowEvent> {

    private final RoomEventService roomEventService;

    @Override
    public void accept(RouletteShowEvent event) {
        roomEventService.showRoulette(event);
    }
}
