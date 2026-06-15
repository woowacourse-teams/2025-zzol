package coffeeshout.room.infra.websocket.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verifyNoInteractions;

import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.room.infra.messaging.RoomStreamKey;
import coffeeshout.websocket.StompSessionManager;
import coffeeshout.websocket.SubscriptionInfoService;
import coffeeshout.websocket.metric.WebSocketMetricService;
import coffeeshout.websocket.ratelimit.WebSocketRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@ExtendWith(MockitoExtension.class)
class SessionDisconnectEventListenerTest {

    @Mock
    StreamPublisher streamPublisher;
    @Mock
    SubscriptionInfoService subscriptionInfoService;
    @Mock
    WebSocketMetricService metricService;
    @Mock
    WebSocketRateLimiter webSocketRateLimiter;

    StompSessionManager sessionManager;
    SessionDisconnectEventListener listener;

    final String sessionId = "test-session-id";
    final String joinCode = "TEST3";
    final String playerName = "testPlayer";

    @BeforeEach
    void setUp() {
        sessionManager = new StompSessionManager();
        final var publisher = Mockito.mock(org.springframework.context.ApplicationEventPublisher.class);
        listener = new SessionDisconnectEventListener(sessionManager, streamPublisher,
                subscriptionInfoService, metricService, webSocketRateLimiter, publisher);
    }

    @Nested
    class 세션_해제_감지 {

        @Test
        void 일반_세션_해제_시_플레이어_처리를_하지_않는다() {
            SessionDisconnectEvent event = createSessionDisconnectEvent(sessionId);

            listener.handleSessionDisconnectEvent(event);

            verifyNoInteractions(streamPublisher);
        }

        @Test
        void 플레이어_세션_해제_시_지연_삭제를_스케줄링한다() {
            sessionManager.registerPlayerSession(joinCode, playerName, sessionId);
            SessionDisconnectEvent event = createSessionDisconnectEvent(sessionId);

            listener.handleSessionDisconnectEvent(event);

            then(streamPublisher).should()
                    .publish(eq(RoomStreamKey.BROADCAST), any());
        }

        @Test
        void 이미_처리된_세션의_중복_해제를_무시한다() {
            sessionManager.registerPlayerSession(joinCode, playerName, sessionId);
            sessionManager.isDisconnectionProcessed(sessionId);
            SessionDisconnectEvent event = createSessionDisconnectEvent(sessionId);

            listener.handleSessionDisconnectEvent(event);

            verifyNoInteractions(streamPublisher);
        }
    }

    private SessionDisconnectEvent createSessionDisconnectEvent(String sessionId) {
        Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).build();
        return new SessionDisconnectEvent(this, message, sessionId, CloseStatus.NORMAL, null);
    }
}
