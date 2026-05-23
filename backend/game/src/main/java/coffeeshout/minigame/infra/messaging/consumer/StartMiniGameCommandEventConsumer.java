package coffeeshout.minigame.infra.messaging.consumer;

import coffeeshout.minigame.application.MiniGameEventService;
import coffeeshout.minigame.event.StartMiniGameCommandEvent;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartMiniGameCommandEventConsumer implements Consumer<StartMiniGameCommandEvent> {

    private final MiniGameEventService miniGameEventService;

    @Override
    public void accept(StartMiniGameCommandEvent event) {
        miniGameEventService.startMiniGame(event);
    }
}
