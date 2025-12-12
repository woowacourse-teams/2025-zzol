package coffeeshout.room.infra.messaging.handler;

import coffeeshout.global.redis.EventHandler;
import coffeeshout.global.ui.WebSocketResponse;
import coffeeshout.global.websocket.LoggingSimpMessagingTemplate;
import coffeeshout.room.domain.event.RouletteSpinEvent;
import coffeeshout.room.domain.player.Winner;
import coffeeshout.room.ui.response.WinnerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RouletteSpinEventHandler implements EventHandler<RouletteSpinEvent> {

    private final LoggingSimpMessagingTemplate messagingTemplate;
    private final RoulettePersistenceService roulettePersistenceService;

    @Override
    public void handle(RouletteSpinEvent event) {
        final Winner winner = event.winner();
        final WinnerResponse response = WinnerResponse.from(winner);

        messagingTemplate.convertAndSend("/topic/room/" + event.joinCode() + "/winner",
                WebSocketResponse.success(response));
        roulettePersistenceService.saveRouletteResult(event);
    }

    @Override
    public Class<RouletteSpinEvent> eventType() {
        return RouletteSpinEvent.class;
    }
}
