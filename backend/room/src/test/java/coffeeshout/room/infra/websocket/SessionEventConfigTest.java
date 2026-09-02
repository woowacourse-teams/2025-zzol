package coffeeshout.room.infra.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.room.infra.messaging.RoomStreamKey;
import coffeeshout.websocket.StompSessionManager;
import coffeeshout.websocket.event.player.PlayerReconnectedEvent;
import coffeeshout.websocket.event.session.SessionRegisteredEvent;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SessionEventConfigTest {

    @Mock
    StreamPublisher streamPublisher;

    StompSessionManager sessionManager;
    Consumer<SessionRegisteredEvent> consumer;

    final String joinCode = "ABC23";
    final String playerName = "김철수";
    final String playerKey = joinCode + ":" + playerName;

    @BeforeEach
    void setUp() {
        sessionManager = new StompSessionManager();
        consumer = new SessionEventConfig(sessionManager, streamPublisher).sessionRegisteredEventConsumer();
    }

    @Nested
    class 세션_등록_이벤트_처리 {

        @Test
        void 처음_등록하는_플레이어는_재접속으로_보지_않는다() {
            consumer.accept(SessionRegisteredEvent.create(playerKey, "session-1"));

            then(streamPublisher).should(never()).publish(eq(RoomStreamKey.BROADCAST), any());
            assertThat(sessionManager.getSessionId(joinCode, playerName)).isEqualTo("session-1");
        }

        @Test
        void 매핑이_남아_있으면_재접속으로_보고_이벤트를_발행한다() {
            sessionManager.registerPlayerSession(joinCode, playerName, "session-1");

            consumer.accept(SessionRegisteredEvent.create(playerKey, "session-2"));

            then(streamPublisher).should().publish(eq(RoomStreamKey.BROADCAST), any(PlayerReconnectedEvent.class));
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
