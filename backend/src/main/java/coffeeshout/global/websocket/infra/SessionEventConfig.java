package coffeeshout.global.websocket.infra;

import coffeeshout.global.redis.stream.StreamKey;
import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.global.websocket.StompSessionManager;
import coffeeshout.global.websocket.event.player.PlayerReconnectedEvent;
import coffeeshout.global.websocket.event.session.SessionRegisteredEvent;
import coffeeshout.global.websocket.event.session.SessionRemovedEvent;
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
                streamPublisher.publish(StreamKey.ROOM_BROADCAST, playerReconnectedEvent);
            }

            sessionManager.registerPlayerSession(playerKey, sessionId);
        };
    }

    @Bean
    public Consumer<SessionRemovedEvent> sessionRemovedEventConsumer() {
        return event -> {
            final String sessionId = event.sessionId();

            log.info("세션 제거 이벤트 처리: sessionId={}", sessionId);

            // 모든 인스턴스가 세션 매핑 제거
            sessionManager.removeSessionInternal(sessionId);
        };
    }

}
