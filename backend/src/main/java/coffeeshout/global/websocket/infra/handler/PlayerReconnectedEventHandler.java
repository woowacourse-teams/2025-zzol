package coffeeshout.global.websocket.infra.handler;

import coffeeshout.global.websocket.DelayedPlayerRemovalService;
import coffeeshout.global.websocket.event.player.PlayerEventType;
import coffeeshout.global.websocket.event.player.PlayerReconnectedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlayerReconnectedEventHandler implements PlayerEventHandler<PlayerReconnectedEvent> {

    private final DelayedPlayerRemovalService delayedPlayerRemovalService;

    @Override
    public void handle(PlayerReconnectedEvent event) {
        final String playerKey = event.playerKey();
        final String sessionId = event.sessionId();
        
        log.info("플레이어 재연결 이벤트 처리: playerKey={}, sessionId={}",
                playerKey, sessionId);

        // 지연 삭제 취소
        delayedPlayerRemovalService.cancelScheduledRemoval(playerKey);
    }

    @Override
    public PlayerEventType getSupportedEventType() {
        return PlayerEventType.PLAYER_RECONNECTED;
    }
}
