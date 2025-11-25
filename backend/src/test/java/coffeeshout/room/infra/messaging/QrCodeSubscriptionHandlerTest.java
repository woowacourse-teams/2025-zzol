package coffeeshout.room.infra.messaging;

import static coffeeshout.room.domain.QrCodeStatus.PENDING;
import static coffeeshout.room.domain.QrCodeStatus.SUCCESS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;

import coffeeshout.global.ui.WebSocketResponse;
import coffeeshout.global.websocket.LoggingSimpMessagingTemplate;
import coffeeshout.room.application.RoomService;
import coffeeshout.room.infra.messaging.handler.QrCodeSubscriptionHandler;
import coffeeshout.room.ui.response.QrCodeStatusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@ExtendWith(MockitoExtension.class)
class QrCodeSubscriptionHandlerTest {

    @Mock
    private RoomService roomService;

    @Mock
    private LoggingSimpMessagingTemplate messagingTemplate;

    private QrCodeSubscriptionHandler qrCodeSubscriptionHandler;

    @BeforeEach
    void setUp() {
        final PathMatcher pathMatcher = new AntPathMatcher();
        qrCodeSubscriptionHandler = new QrCodeSubscriptionHandler(roomService, messagingTemplate, pathMatcher);
    }

    @Test
    @DisplayName("QR 코드 토픽 구독 시 현재 상태를 구독자에게 전송한다")
    void QR_코드_토픽_구독_시_현재_상태를_구독자에게_전송한다() {
        // given
        String joinCode = "ABCD";
        String sessionId = "test-session-123";
        String destination = "/topic/room/" + joinCode + "/qr-code";

        QrCodeStatusResponse response = new QrCodeStatusResponse(PENDING, null);
        given(roomService.getQrCodeStatus(joinCode)).willReturn(response);

        Message<byte[]> message = createSubscribeMessage(sessionId, destination);
        SessionSubscribeEvent event = new SessionSubscribeEvent(this, message);

        // when
        qrCodeSubscriptionHandler.handleSubscribeQrCodeStatus(event);

        // then
        then(roomService).should(times(1)).getQrCodeStatus(joinCode);
        then(messagingTemplate).should(timeout(500).times(1))
                .convertAndSend(eq(destination), eq(WebSocketResponse.success(response)));
    }

    @Test
    @DisplayName("SUCCESS 상태의 QR 코드를 구독자에게 전송한다")
    void SUCCESS_상태의_QR_코드를_구독자에게_전송한다() {
        // given
        String joinCode = "WXYZ";
        String sessionId = "test-session-456";
        String destination = "/topic/room/" + joinCode + "/qr-code";
        String qrCodeUrl = "https://example.com/qr-code.png";

        QrCodeStatusResponse response = new QrCodeStatusResponse(SUCCESS, qrCodeUrl);
        given(roomService.getQrCodeStatus(joinCode)).willReturn(response);

        Message<byte[]> message = createSubscribeMessage(sessionId, destination);
        SessionSubscribeEvent event = new SessionSubscribeEvent(this, message);

        // when
        qrCodeSubscriptionHandler.handleSubscribeQrCodeStatus(event);

        // then
        then(roomService).should(times(1)).getQrCodeStatus(joinCode);
        then(messagingTemplate).should(timeout(500).times(1))
                .convertAndSend(eq(destination), eq(WebSocketResponse.success(response)));
    }

    @Test
    @DisplayName("QR 코드 토픽이 아닌 경로 구독 시 메시지를 전송하지 않는다")
    void QR_코드_토픽이_아닌_경로_구독_시_메시지를_전송하지_않는다() {
        // given
        String sessionId = "test-session-789";
        String destination = "/topic/room/other-topic";

        Message<byte[]> message = createSubscribeMessage(sessionId, destination);
        SessionSubscribeEvent event = new SessionSubscribeEvent(this, message);

        // when
        qrCodeSubscriptionHandler.handleSubscribeQrCodeStatus(event);

        // then
        then(roomService).should(never()).getQrCodeStatus(any());
        then(messagingTemplate).should(never()).convertAndSendToUser(any(), any(), any());
    }

    @Test
    @DisplayName("destination이 null인 경우 메시지를 전송하지 않는다")
    void destination이_null인_경우_메시지를_전송하지_않는다() {
        // given
        String sessionId = "test-session-null";

        Message<byte[]> message = createSubscribeMessage(sessionId, null);
        SessionSubscribeEvent event = new SessionSubscribeEvent(this, message);

        // when
        qrCodeSubscriptionHandler.handleSubscribeQrCodeStatus(event);

        // then
        then(roomService).should(never()).getQrCodeStatus(any());
        then(messagingTemplate).should(never()).convertAndSendToUser(any(), any(), any());
    }

    @Test
    @DisplayName("잘못된 형식의 joinCode를 가진 경로는 매칭되지 않는다")
    void 잘못된_형식의_joinCode를_가진_경로는_매칭되지_않는다() {
        // given
        String sessionId = "test-session-invalid";
        String destination = "/topic/room/ABC/qr-code"; // 3자리 (4자리가 아님)

        Message<byte[]> message = createSubscribeMessage(sessionId, destination);
        SessionSubscribeEvent event = new SessionSubscribeEvent(this, message);

        // when
        qrCodeSubscriptionHandler.handleSubscribeQrCodeStatus(event);

        // then
        then(roomService).should(never()).getQrCodeStatus(any());
        then(messagingTemplate).should(never()).convertAndSendToUser(any(), any(), any());
    }

    @Test
    @DisplayName("sessionId가 null인 경우 메시지를 전송하지 않는다")
    void sessionId가_null인_경우_메시지를_전송하지_않는다() {
        // given
        String joinCode = "ABCD";
        String destination = "/topic/room/" + joinCode + "/qr-code";

        Message<byte[]> message = createSubscribeMessage(null, destination);  // sessionId를 null로
        SessionSubscribeEvent event = new SessionSubscribeEvent(this, message);

        // when
        qrCodeSubscriptionHandler.handleSubscribeQrCodeStatus(event);

        // then
        // getQrCodeStatus는 호출되지만, 메시지 전송은 안됨
        then(roomService).should(times(0)).getQrCodeStatus(joinCode);
        then(messagingTemplate).should(never()).convertAndSendToUser(any(), any(), any());
    }

    private Message<byte[]> createSubscribeMessage(String sessionId, String destination) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.SUBSCRIBE);
        accessor.setSessionId(sessionId);
        accessor.setDestination(destination);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
