package coffeeshout.room.infra.messaging.handler;

import coffeeshout.global.ui.WebSocketResponse;
import coffeeshout.global.websocket.LoggingSimpMessagingTemplate;
import coffeeshout.room.domain.event.RouletteSpinEvent;
import coffeeshout.room.domain.player.Winner;
import coffeeshout.room.ui.response.WinnerResponse;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RouletteSpinEventHandler implements Consumer<RouletteSpinEvent> {

    private final RoulettePersistenceService roulettePersistenceService;
    private final LoggingSimpMessagingTemplate messagingTemplate;

    @Override
    public void accept(RouletteSpinEvent event) {
        final Winner winner = event.winner();
        final WinnerResponse response = WinnerResponse.from(winner);

        messagingTemplate.convertAndSend("/topic/room/" + event.joinCode() + "/winner",
                WebSocketResponse.success(response));
        roulettePersistenceService.saveRouletteResult(event);
    }
}
