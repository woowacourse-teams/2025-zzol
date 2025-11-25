package coffeeshout.global.websocket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verifyNoInteractions;

import coffeeshout.global.metric.WebSocketMetricService;
import coffeeshout.global.websocket.event.SessionDisconnectEventListener;
import coffeeshout.global.websocket.infra.PlayerEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@ExtendWith(MockitoExtension.class)
class SessionDisconnectEventListenerTest {

    @Mock
    PlayerEventPublisher playerEventPublisher;
    @Mock
    SubscriptionInfoService subscriptionInfoService;
    @Mock
    WebSocketMetricService metricService;

    StompSessionManager sessionManager;
    SessionDisconnectEventListener listener;

    final String sessionId = "test-session-id";
    final String joinCode = "TEST3";
    final String playerName = "testPlayer";

    @BeforeEach
    void setUp() {
        sessionManager = new StompSessionManager();
        listener = new SessionDisconnectEventListener(sessionManager, playerEventPublisher,
                subscriptionInfoService, metricService);
    }

    @Nested
    class 세션_해제_감지 {

        @Test
        void 일반_세션_해제_시_플레이어_처리를_하지_않는다() {
            // given
            SessionDisconnectEvent event = createSessionDisconnectEvent(sessionId);

            // when
            listener.handleSessionDisconnectEvent(event);

            // then
            verifyNoInteractions(playerEventPublisher);
        }

        @Test
        void 플레이어_세션_해제_시_지연_삭제를_스케줄링한다() {
            // given
            sessionManager.registerPlayerSession(joinCode, playerName, sessionId);
            SessionDisconnectEvent event = createSessionDisconnectEvent(sessionId);

            // when
            listener.handleSessionDisconnectEvent(event);

            // then
            then(playerEventPublisher).should()
                    .publishEvent(any());
        }

        @Test
        void 이미_처리된_세션의_중복_해제를_무시한다() {
            // given
            sessionManager.registerPlayerSession(joinCode, playerName, sessionId);
            // 이미 처리된 상태로 설정
            sessionManager.isDisconnectionProcessed(sessionId);
            SessionDisconnectEvent event = createSessionDisconnectEvent(sessionId);

            // when
            listener.handleSessionDisconnectEvent(event);

            // then
            verifyNoInteractions(playerEventPublisher);
        }
    }

    private SessionDisconnectEvent createSessionDisconnectEvent(String sessionId) {
        Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).build();
        return new SessionDisconnectEvent(this, message, sessionId, CloseStatus.NORMAL, null);
    }
}
