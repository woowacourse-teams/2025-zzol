package coffeeshout.numberpoker.infra.messaging.consumer;

import coffeeshout.numberpoker.application.NumberPokerService;
import coffeeshout.numberpoker.infra.messaging.event.SettingsCommandEvent;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SettingsCommandEventConsumer implements Consumer<SettingsCommandEvent> {

    private final NumberPokerService numberPokerService;

    @Override
    public void accept(SettingsCommandEvent event) {
        numberPokerService.configureRoundCount(event.joinCode(), event.hostName(), event.roundCount());
    }
}
