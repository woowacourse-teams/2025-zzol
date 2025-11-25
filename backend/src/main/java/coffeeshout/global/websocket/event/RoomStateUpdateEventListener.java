package coffeeshout.global.websocket.event;

import coffeeshout.room.application.RoomService;
import coffeeshout.room.domain.event.PlayerListUpdateEvent;
import coffeeshout.room.infra.messaging.RoomEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomStateUpdateEventListener {

    private final RoomService roomService;
    private final RoomEventPublisher roomEventPublisher;

    @EventListener
    public void handleRoomStateUpdate(RoomStateUpdateEvent event) {
        try {
            log.info("방 상태 업데이트 이벤트 처리: joinCode={}, reason={}", event.joinCode(), event.reason());

            broadcastRoomState(event.joinCode());
        } catch (Exception e) {
            log.error("방 상태 업데이트 이벤트 처리 실패: joinCode={}, reason={}", event.joinCode(), event.reason(), e);
        }
    }

    private void broadcastRoomState(String joinCode) {
        if (roomService.roomExists(joinCode)) {
            sendPlayerStatus(joinCode);
            log.info("방 상태 브로드캐스트 완료: joinCode={}", joinCode);
        }
    }

    private void sendPlayerStatus(String joinCode) {
        final PlayerListUpdateEvent event = new PlayerListUpdateEvent(joinCode);
        roomEventPublisher.publishEvent(event);
    }
}
