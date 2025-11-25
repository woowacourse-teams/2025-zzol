package coffeeshout.room.infra.messaging.handler;

import coffeeshout.global.ui.WebSocketResponse;
import coffeeshout.global.websocket.LoggingSimpMessagingTemplate;
import coffeeshout.room.application.RoomService;
import coffeeshout.room.domain.event.PlayerKickEvent;
import coffeeshout.room.domain.event.RoomEventType;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.ui.response.PlayerResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlayerKickEventHandler implements RoomEventHandler<PlayerKickEvent> {

    private final LoggingSimpMessagingTemplate messagingTemplate;
    private final RoomService roomService;

    @Override
    public void handle(PlayerKickEvent event) {
        try {
            log.info("플레이어 강퇴 이벤트 수신: eventId={}, joinCode={}, playerName={}",
                    event.eventId(), event.joinCode(), event.playerName());

            final boolean removed = roomService.removePlayer(event.joinCode(), event.playerName());
            if (!removed) {
                log.warn("플레이어 강퇴 실패 - 플레이어 없음: eventId={}, joinCode={}, playerName={}",
                        event.eventId(), event.joinCode(), event.playerName());
                // 실패 응답 보내거나, 아니면 그냥 리턴
                return;
            }

            final List<Player> players = roomService.getPlayersInternal(event.joinCode());
            final List<PlayerResponse> responses = players.stream()
                    .map(PlayerResponse::from)
                    .toList();

            messagingTemplate.convertAndSend(
                    "/topic/room/" + event.joinCode(),
                    WebSocketResponse.success(responses)
            );
            log.info("플레이어 강퇴 이벤트 처리 완료: eventId={}, joinCode={}, playerName={}",
                    event.eventId(), event.joinCode(), event.playerName());

        } catch (Exception e) {
            log.error("플레이어 강퇴 이벤트 처리 실패", e);
            throw e;
        }
    }

    @Override
    public RoomEventType getSupportedEventType() {
        return RoomEventType.PLAYER_KICK;
    }
}
