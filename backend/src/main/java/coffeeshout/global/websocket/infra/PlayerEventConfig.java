package coffeeshout.global.websocket.infra;

import coffeeshout.global.websocket.DelayedPlayerRemovalService;
import coffeeshout.global.websocket.event.player.PlayerDisconnectedEvent;
import coffeeshout.global.websocket.event.player.PlayerReconnectedEvent;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class PlayerEventConfig {

    private final DelayedPlayerRemovalService delayedPlayerRemovalService;

    @Bean
    public Consumer<PlayerDisconnectedEvent> disconnectedEventConsumer() {
        return event -> {
            final String playerKey = event.playerKey();
            final String sessionId = event.sessionId();
            final String reason = event.reason();

            log.info("플레이어 연결 해제 이벤트 처리: playerKey={}, sessionId={}, reason={}",
                    playerKey, sessionId, reason);

            delayedPlayerRemovalService.schedulePlayerRemoval(playerKey, sessionId, reason);
        };
    }

    @Bean
    public Consumer<PlayerReconnectedEvent> reconnectedEventConsumer() {
        return event -> {
            final String playerKey = event.playerKey();
            final String sessionId = event.sessionId();

            log.info("플레이어 재연결 이벤트 처리: playerKey={}, sessionId={}",
                    playerKey, sessionId);

            // 지연 삭제 취소
            delayedPlayerRemovalService.cancelScheduledRemoval(playerKey);
        };
    }

}
