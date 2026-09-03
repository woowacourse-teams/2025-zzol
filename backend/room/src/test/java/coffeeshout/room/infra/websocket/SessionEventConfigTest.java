package coffeeshout.room.infra.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.websocket.StompSessionManager;
import coffeeshout.websocket.event.session.SessionRegisteredEvent;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SessionEventConfigTest {

    StompSessionManager sessionManager;
    Consumer<SessionRegisteredEvent> consumer;

    final String joinCode = "ABC23";
    final String playerName = "김철수";
    final String playerKey = joinCode + ":" + playerName;

    @BeforeEach
    void setUp() {
        sessionManager = new StompSessionManager();
        consumer = new SessionEventConfig(sessionManager).sessionRegisteredEventConsumer();
    }

    @Nested
    class 세션_등록_이벤트_처리 {

        @Test
        void 다른_인스턴스의_매핑_복제본을_만든다() {
            consumer.accept(SessionRegisteredEvent.create(playerKey, "session-1"));

            assertThat(sessionManager.getSessionId(joinCode, playerName)).isEqualTo("session-1");
        }

        @Test
        void 재접속하면_새_세션으로_매핑을_갈아끼운다() {
            sessionManager.registerPlayerSession(joinCode, playerName, "session-1");

            consumer.accept(SessionRegisteredEvent.create(playerKey, "session-2"));

            assertThat(sessionManager.getSessionId(joinCode, playerName)).isEqualTo("session-2");
            assertThat(sessionManager.hasPlayerKey("session-1")).isFalse();
        }
    }
}
