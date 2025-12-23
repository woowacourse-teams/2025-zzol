package coffeeshout.room.infra.messaging.handler;

import coffeeshout.global.ui.WebSocketResponse;
import coffeeshout.global.websocket.LoggingSimpMessagingTemplate;
import coffeeshout.room.application.RoomService;
import coffeeshout.room.domain.event.PlayerReadyEvent;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.ui.response.PlayerResponse;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlayerReadyEventHandler implements Consumer<PlayerReadyEvent> {

    private final RoomService roomService;
    private final LoggingSimpMessagingTemplate messagingTemplate;

    @Override
    public void accept(PlayerReadyEvent event) {
        final List<Player> players = roomService.changePlayerReadyStateInternal(
                event.joinCode(),
                event.playerName(),
                event.isReady()
        );
        final List<PlayerResponse> responses = players.stream()
                .map(PlayerResponse::from)
                .toList();

        messagingTemplate.convertAndSend("/topic/room/" + event.joinCode(),
                WebSocketResponse.success(responses));
    }
}
