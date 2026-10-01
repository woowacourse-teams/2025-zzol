package coffeeshout.room.infra.websocket;

import coffeeshout.websocket.StompSessionManager;
import coffeeshout.websocket.event.session.SessionRegisteredEvent;
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

    /**
     * 다른 인스턴스의 매핑 복제본을 맞춘다. 재접속 판정과 `PlayerReconnectedEvent` 발행은 소켓을 쥔
     * 인스턴스가 이미 했다. 여기서 판정하면 이 스트림을 모든 인스턴스가 읽으므로 재접속 한 번에
     * 이벤트가 인스턴스 수만큼 나간다.
     */
    @Bean
    public Consumer<SessionRegisteredEvent> sessionRegisteredEventConsumer() {
        return event -> {
            log.info("세션 등록 이벤트 처리: playerKey={}, sessionId={}", event.playerKey(), event.sessionId());
            sessionManager.registerPlayerSession(event.playerKey(), event.sessionId());
        };
    }
}
