package coffeeshout.room.infra.messaging.consumer;

import coffeeshout.room.application.service.RoomService;
import coffeeshout.room.domain.event.RouletteShowEvent;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RouletteShowConsumer implements Consumer<RouletteShowEvent> {

    private final RoomService roomService;

    @Override
    public void accept(RouletteShowEvent event) {
        roomService.showRoulette(event);
    }
}
