package coffeeshout.room.infra.messaging.handler;

import coffeeshout.global.redis.EventHandler;
import coffeeshout.global.ui.WebSocketResponse;
import coffeeshout.global.websocket.LoggingSimpMessagingTemplate;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.application.RoomService;
import coffeeshout.room.domain.event.MiniGameSelectEvent;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MiniGameSelectEventHandler implements EventHandler<MiniGameSelectEvent> {

    private final RoomService roomService;
    private final LoggingSimpMessagingTemplate messagingTemplate;

    @Override
    public void handle(MiniGameSelectEvent event) {
        final List<MiniGameType> selectedMiniGames = roomService.updateMiniGamesInternal(
                event.joinCode(),
                event.hostName(),
                event.miniGameTypes()
        );

        messagingTemplate.convertAndSend("/topic/room/" + event.joinCode() + "/minigame",
                WebSocketResponse.success(selectedMiniGames));
    }

    @Override
    public Class<MiniGameSelectEvent> eventType() {
        return MiniGameSelectEvent.class;
    }
}
