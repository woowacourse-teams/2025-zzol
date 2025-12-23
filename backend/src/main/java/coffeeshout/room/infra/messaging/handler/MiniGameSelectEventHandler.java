package coffeeshout.room.infra.messaging.handler;

import coffeeshout.global.ui.WebSocketResponse;
import coffeeshout.global.websocket.LoggingSimpMessagingTemplate;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.application.RoomService;
import coffeeshout.room.domain.event.MiniGameSelectEvent;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MiniGameSelectEventHandler implements Consumer<MiniGameSelectEvent> {

    private final RoomService roomService;
    private final LoggingSimpMessagingTemplate messagingTemplate;

    @Override
    public void accept(MiniGameSelectEvent event) {
        final List<MiniGameType> selectedMiniGames =
                roomService.updateMiniGamesInternal(
                        event.joinCode(),
                        event.hostName(),
                        event.miniGameTypes()
                );

        messagingTemplate.convertAndSend("/topic/room/" + event.joinCode() + "/minigame",
                WebSocketResponse.success(selectedMiniGames));
    }
}
