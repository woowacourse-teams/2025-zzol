package coffeeshout.numberpoker.infra.messaging.consumer;

import coffeeshout.numberpoker.application.NumberPokerService;
import coffeeshout.numberpoker.infra.messaging.event.FoldCommandEvent;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FoldCommandEventConsumer implements Consumer<FoldCommandEvent> {

    private final NumberPokerService numberPokerService;

    @Override
    public void accept(FoldCommandEvent event) {
        numberPokerService.fold(event.joinCode(), event.playerName());
    }
}
