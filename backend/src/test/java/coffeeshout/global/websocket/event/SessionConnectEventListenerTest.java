package coffeeshout.global.websocket.event;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.global.metric.WebSocketMetricService;
import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.redis.stream.StreamKey;
import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.room.domain.RoomErrorCode;
import coffeeshout.room.domain.service.RoomQueryService;
import java.security.Principal;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.web.socket.messaging.SessionConnectedEvent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SessionConnectEventListenerTest {

    @Mock
    private WebSocketMetricService webSocketMetricService;
    @Mock
    private StreamPublisher streamPublisher;
    @Mock
    private RoomQueryService roomQueryService;

    private SessionConnectEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new SessionConnectEventListener(webSocketMetricService, streamPublisher, roomQueryService);
    }

    private SessionConnectedEvent eventWith(String principalName) {
        final MessageHeaders headers = new MessageHeaders(Map.of("simpSessionId", "session-1"));
        final Message<byte[]> message = new GenericMessage<>(new byte[0], headers);
        return new SessionConnectedEvent(this, message, () -> principalName);
    }

    @Nested
    class 룸_세션_토큰으로_연결한_경우 {

        @Test
        void 유효한_PlayerKey_형식이면_룸_조회와_세션_등록_이벤트를_발행한다() {
            final SessionConnectedEvent event = eventWith("ABCD:홍길동");

            listener.handleSessionConnected(event);

            verify(roomQueryService).getByJoinCode(any());
            verify(streamPublisher).publish(eq(StreamKey.ROOM_BROADCAST), any(BaseEvent.class));
            verify(webSocketMetricService).completeConnection("session-1");
        }
    }

    @Nested
    class Bearer_토큰으로_연결한_경우 {

        @Test
        void user_userId_형식_Principal이면_룸_처리를_건너뛴다() {
            final SessionConnectedEvent event = eventWith("user:4");

            listener.handleSessionConnected(event);

            verify(roomQueryService, never()).getByJoinCode(any());
            verify(streamPublisher, never()).publish(any(), any());
            verify(webSocketMetricService).completeConnection("session-1");
        }
    }

    @Nested
    class Principal_형식이_올바르지_않은_경우 {

        @Test
        void PlayerKey_형식이_아니면_룸_처리를_건너뛴다() {
            final SessionConnectedEvent event = eventWith("b1c2d3e4-f5a6-7890-abcd-ef1234567890");

            listener.handleSessionConnected(event);

            verify(roomQueryService, never()).getByJoinCode(any());
            verify(streamPublisher, never()).publish(any(), any());
            verify(webSocketMetricService).completeConnection("session-1");
        }

        @Test
        void Principal이_null이면_룸_처리를_건너뛴다() {
            final MessageHeaders headers = new MessageHeaders(Map.of("simpSessionId", "session-1"));
            final Message<byte[]> message = new GenericMessage<>(new byte[0], headers);
            final SessionConnectedEvent event = new SessionConnectedEvent(this, message, null);

            listener.handleSessionConnected(event);

            verify(roomQueryService, never()).getByJoinCode(any());
            verify(streamPublisher, never()).publish(any(), any());
            verify(webSocketMetricService).completeConnection("session-1");
        }
    }

    @Nested
    class 룸_조회가_실패한_경우 {

        @Test
        void 세션_등록_이벤트를_발행하지_않고_메트릭만_완료한다() {
            given(roomQueryService.getByJoinCode(any()))
                    .willThrow(new BusinessException(RoomErrorCode.ROOM_NOT_FOUND, "방을 찾을 수 없습니다."));
            final SessionConnectedEvent event = eventWith("ABCD:홍길동");

            listener.handleSessionConnected(event);

            verify(streamPublisher, never()).publish(any(), any());
            verify(webSocketMetricService).completeConnection("session-1");
        }
    }
}
