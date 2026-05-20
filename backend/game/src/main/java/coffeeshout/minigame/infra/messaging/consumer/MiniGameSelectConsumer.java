package coffeeshout.minigame.infra.messaging.consumer;

import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.room.domain.event.MiniGameSelectEvent;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MiniGameSelectConsumer implements Consumer<MiniGameSelectEvent> {

    private final GameSessionService gameSessionService;

    @Override
    public void accept(MiniGameSelectEvent event) {
        gameSessionService.updateGames(event);
    }
}
