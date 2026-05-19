package coffeeshout.room.infra;

import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.room.infra.messaging.RoomStreamKey;
import coffeeshout.websocket.StompSessionManager;
import coffeeshout.websocket.event.player.PlayerReconnectedEvent;
import coffeeshout.websocket.event.session.SessionRegisteredEvent;
import coffeeshout.websocket.event.session.SessionRemovedEvent;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class SessionEventConfig {

    private final StompSessionManager sessionManager;
    private final StreamPublisher streamPublisher;

    @Bean
    public Consumer<SessionRegisteredEvent> sessionRegisteredEventConsumer() {
        return event -> {
            final String playerKey = event.playerKey();
            final String sessionId = event.sessionId();

            log.info("세션 등록 이벤트 처리: playerKey={}, sessionId={}", playerKey, sessionId);

            if (sessionManager.hasPlayerKeyInternal(playerKey)) {
                log.info("플레이어 재연결 감지: playerKey={}, sessionId={}", playerKey, sessionId);

                final PlayerReconnectedEvent playerReconnectedEvent = PlayerReconnectedEvent.create(playerKey, sessionId);
                streamPublisher.publish(RoomStreamKey.BROADCAST, playerReconnectedEvent);
            }

            sessionManager.registerPlayerSession(playerKey, sessionId);
        };
    }

    @Bean
    public Consumer<SessionRemovedEvent> sessionRemovedEventConsumer() {
        return event -> {
            final String sessionId = event.sessionId();

            log.info("세션 제거 이벤트 처리: sessionId={}", sessionId);

            sessionManager.removeSession(sessionId);
        };
    }
}
