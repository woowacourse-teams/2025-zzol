package coffeeshout.room.infra.messaging.consumer;

import coffeeshout.room.application.service.RoomEventService;
import coffeeshout.room.domain.event.RouletteSpinEvent;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RouletteSpinConsumer implements Consumer<RouletteSpinEvent> {

    private final RoomEventService roomEventService;

    @Override
    public void accept(RouletteSpinEvent event) {
        roomEventService.spinRoulette(event);
    }
}
