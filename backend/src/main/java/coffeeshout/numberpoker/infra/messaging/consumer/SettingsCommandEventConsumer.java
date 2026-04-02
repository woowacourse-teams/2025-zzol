package coffeeshout.numberpoker.infra.messaging.consumer;

import coffeeshout.numberpoker.application.NumberPokerService;
import coffeeshout.numberpoker.infra.messaging.event.SettingsCommandEvent;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettingsCommandEventConsumer implements Consumer<SettingsCommandEvent> {

    private final NumberPokerService numberPokerService;

    @Override
    public void accept(SettingsCommandEvent event) {
        log.debug("라운드 설정 이벤트 처리: joinCode={}, host={}, roundCount={}",
                event.joinCode(), event.hostName(), event.roundCount());
        numberPokerService.configureRoundCount(event.joinCode(), event.hostName(), event.roundCount());
    }
}
