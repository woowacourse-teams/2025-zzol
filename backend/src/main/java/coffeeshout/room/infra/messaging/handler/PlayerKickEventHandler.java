package coffeeshout.room.infra.messaging.handler;

import coffeeshout.global.redis.EventHandler;
import coffeeshout.global.ui.WebSocketResponse;
import coffeeshout.global.websocket.LoggingSimpMessagingTemplate;
import coffeeshout.room.application.RoomService;
import coffeeshout.room.domain.event.PlayerKickEvent;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.ui.response.PlayerResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlayerKickEventHandler implements EventHandler<PlayerKickEvent> {

    private final LoggingSimpMessagingTemplate messagingTemplate;
    private final RoomService roomService;

    @Override
    public void handle(PlayerKickEvent event) {
        roomService.removePlayer(event.joinCode(), event.playerName());

        final List<Player> players = roomService.getPlayersInternal(event.joinCode());
        final List<PlayerResponse> responses = players.stream()
                .map(PlayerResponse::from)
                .toList();

        messagingTemplate.convertAndSend(
                "/topic/room/" + event.joinCode(),
                WebSocketResponse.success(responses)
        );
    }

    @Override
    public Class<PlayerKickEvent> eventType() {
        return PlayerKickEvent.class;
    }
}
