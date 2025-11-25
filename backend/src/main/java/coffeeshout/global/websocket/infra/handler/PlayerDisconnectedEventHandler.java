package coffeeshout.global.websocket.infra.handler;

import coffeeshout.global.websocket.DelayedPlayerRemovalService;
import coffeeshout.global.websocket.event.player.PlayerDisconnectedEvent;
import coffeeshout.global.websocket.event.player.PlayerEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlayerDisconnectedEventHandler implements PlayerEventHandler<PlayerDisconnectedEvent> {

    private final DelayedPlayerRemovalService delayedPlayerRemovalService;

    @Override
    public void handle(PlayerDisconnectedEvent event) {
        final String playerKey = event.playerKey();
        final String sessionId = event.sessionId();
        final String reason = event.reason();
        
        log.info("플레이어 연결 해제 이벤트 처리: playerKey={}, sessionId={}, reason={}",
                playerKey, sessionId, reason);

        // 지연 삭제 스케줄링
        delayedPlayerRemovalService.schedulePlayerRemoval(playerKey, sessionId, reason);
    }

    @Override
    public PlayerEventType getSupportedEventType() {
        return PlayerEventType.PLAYER_DISCONNECTED;
    }
}
