package coffeeshout.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.fixture.IntegrationTestSupport;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

class WebSocketConnectionTest extends IntegrationTestSupport {

    @LocalServerPort
    private int port;

    @Test
    void 순수_WebSocket_STOMP_연결_테스트() throws Exception {
        // given
        WebSocketStompClient stompClient = new WebSocketStompClient(new SockJsClient(List.of(
                new WebSocketTransport(new StandardWebSocketClient())
        )));
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        CountDownLatch connectionLatch = new CountDownLatch(1);
        String url = "ws://localhost:" + port + "/ws"; // 순수 WebSocket

        // when
        StompSession session = stompClient.connectAsync(url, new TestConnectionHandler(connectionLatch))
                .get(10, TimeUnit.SECONDS);

        // then
        assertThat(session.isConnected()).isTrue();
        assertThat(connectionLatch.await(5, TimeUnit.SECONDS)).isTrue();

        session.disconnect();
    }

    @Test
    void 토픽_구독_테스트() throws Exception {
        // given
        WebSocketStompClient stompClient = new WebSocketStompClient(new SockJsClient(List.of(
                new WebSocketTransport(new StandardWebSocketClient())
        )));
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        CountDownLatch connectionLatch = new CountDownLatch(1);
        String url = "ws://localhost:" + port + "/ws";

        // when
        StompSession session = stompClient.connectAsync(url, new TestConnectionHandler(connectionLatch))
                .get(10, TimeUnit.SECONDS);

        // 방 토픽 구독
        session.subscribe("/topic/room/1", new TestFrameHandler());

        // then
        assertThat(connectionLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(session.isConnected()).isTrue();

        session.disconnect();
    }

    @Test
    void 메시지_전송_테스트() throws Exception {
        // given
        WebSocketStompClient stompClient = new WebSocketStompClient(new SockJsClient(List.of(
                new WebSocketTransport(new StandardWebSocketClient())
        )));
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        CountDownLatch connectionLatch = new CountDownLatch(1);
        String url = "ws://localhost:" + port + "/ws";

        // when
        StompSession session = stompClient.connectAsync(url, new TestConnectionHandler(connectionLatch))
                .get(10, TimeUnit.SECONDS);

        // 토픽 구독
        session.subscribe("/topic/room/1", new TestFrameHandler());

        // 메시지 전송 (실제 룸이 없어도 메시지 전송 자체는 가능)
        session.send("/app/room/1/players", null);

        // then
        assertThat(connectionLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(session.isConnected()).isTrue();

        Thread.sleep(100); // 메시지 처리 대기
        session.disconnect();
    }

    private static class TestConnectionHandler implements StompSessionHandler {
        private final CountDownLatch latch;
        private boolean connected = false;

        public TestConnectionHandler(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            System.out.println("✅ 순수 WebSocket 연결 성공!");
            connected = true;
            latch.countDown();
        }

        @Override
        public void handleException(StompSession session, StompCommand command,
                                    StompHeaders headers, byte[] payload, Throwable exception) {
            System.err.println("❌ STOMP 에러: " + exception.getMessage());
            exception.printStackTrace();
            latch.countDown();
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            System.err.println("❌ 전송 에러: " + exception.getMessage());
            exception.printStackTrace();
            latch.countDown();
        }

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return String.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            System.out.println("📨 프레임 수신: " + payload);
        }

        public boolean isConnected() {
            return connected;
        }
    }

    private static class TestFrameHandler implements org.springframework.messaging.simp.stomp.StompFrameHandler {

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return String.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            System.out.println("📬 토픽에서 메시지 수신: " + payload);
        }
    }
}
