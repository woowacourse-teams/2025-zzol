package coffeeshout.room.infra.messaging.consumer;

import coffeeshout.room.application.service.RoomService;
import coffeeshout.room.domain.event.RouletteSpinEvent;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RouletteSpinConsumer implements Consumer<RouletteSpinEvent> {

    private final RoomService roomService;

    @Override
    public void accept(RouletteSpinEvent event) {
        roomService.spinRoulette(event);
    }
}
