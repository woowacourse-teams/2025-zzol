package coffeeshout.room.infra.messaging.handler;

import coffeeshout.global.redis.EventHandler;
import coffeeshout.global.ui.WebSocketResponse;
import coffeeshout.global.websocket.LoggingSimpMessagingTemplate;
import coffeeshout.room.application.RoomService;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.event.RouletteShowEvent;
import coffeeshout.room.ui.response.RoomStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RouletteShowEventHandler implements EventHandler<RouletteShowEvent> {

    private final RoomService roomService;
    private final RoulettePersistenceService roulettePersistenceService;
    private final LoggingSimpMessagingTemplate messagingTemplate;

    @Override
    public void handle(RouletteShowEvent event) {
        final Room room = roomService.showRoulette(event.joinCode());
        final RoomStatusResponse response = RoomStatusResponse.of(room.getJoinCode(), room.getRoomState());

        messagingTemplate.convertAndSend("/topic/room/" + event.joinCode() + "/roulette",
                WebSocketResponse.success(response));
        roulettePersistenceService.saveRoomStatus(event);
    }

    @Override
    public Class<RouletteShowEvent> eventType() {
        return RouletteShowEvent.class;
    }
}
