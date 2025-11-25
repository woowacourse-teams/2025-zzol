package coffeeshout.room.infra.messaging.handler;

import coffeeshout.global.ui.WebSocketResponse;
import coffeeshout.global.websocket.LoggingSimpMessagingTemplate;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.application.RoomService;
import coffeeshout.room.domain.event.MiniGameSelectEvent;
import coffeeshout.room.domain.event.RoomEventType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MiniGameSelectEventHandler implements RoomEventHandler<MiniGameSelectEvent> {

    private final RoomService roomService;
    private final LoggingSimpMessagingTemplate messagingTemplate;

    @Override
    public void handle(MiniGameSelectEvent event) {
        try {
            log.info("미니게임 선택 이벤트 수신: eventId={}, joinCode={}, hostName={}, miniGameTypes={}",
                    event.eventId(), event.joinCode(), event.hostName(), event.miniGameTypes());

            final List<MiniGameType> selectedMiniGames = roomService.updateMiniGamesInternal(
                    event.joinCode(),
                    event.hostName(),
                    event.miniGameTypes()
            );

            messagingTemplate.convertAndSend("/topic/room/" + event.joinCode() + "/minigame",
                    WebSocketResponse.success(selectedMiniGames));

            log.info("미니게임 선택 이벤트 처리 완료: eventId={}, joinCode={}, selectedCount={}",
                    event.eventId(), event.joinCode(), selectedMiniGames.size());

        } catch (Exception e) {
            log.error("미니게임 선택 이벤트 처리 실패", e);
        }
    }

    @Override
    public RoomEventType getSupportedEventType() {
        return RoomEventType.MINI_GAME_SELECT;
    }
}
