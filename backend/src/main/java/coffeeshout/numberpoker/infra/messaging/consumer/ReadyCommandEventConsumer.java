package coffeeshout.numberpoker.infra.messaging.consumer;

import coffeeshout.numberpoker.application.NumberPokerService;
import coffeeshout.numberpoker.infra.messaging.event.ReadyCommandEvent;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReadyCommandEventConsumer implements Consumer<ReadyCommandEvent> {

    private final NumberPokerService numberPokerService;

    @Override
    public void accept(ReadyCommandEvent event) {
        numberPokerService.ready(event.joinCode(), event.playerName());
    }
}
