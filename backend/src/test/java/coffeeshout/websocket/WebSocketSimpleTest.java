package coffeeshout.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.support.test.IntegrationTest;
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
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.RestTemplateXhrTransport;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

/**
 * 간단한 웹소켓 연결 및 E2E 테스트 실제 데이터 없이도 웹소켓 연결과 메시지 전송을 테스트
 */
@IntegrationTest
class WebSocketSimpleTest {

    @LocalServerPort
    private int port;

    @Test
    void SockJS_STOMP_엔드포인트_연결_테스트() throws Exception {
        // given
        List<Transport> transports = List.of(
                new WebSocketTransport(new StandardWebSocketClient()),
                new RestTemplateXhrTransport()
        );
        WebSocketClient sockJsClient = new SockJsClient(transports);
        WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        CountDownLatch connectionLatch = new CountDownLatch(1);
        String url = "http://localhost:" + port + "/ws";

        // when
        StompSession session = stompClient.connectAsync(url, new SimpleStompSessionHandler(connectionLatch))
                .get(10, TimeUnit.SECONDS);

        // then
        assertThat(session.isConnected()).isTrue();
        assertThat(connectionLatch.await(5, TimeUnit.SECONDS)).isTrue();

        session.disconnect();
    }

    @Test
    void 방_토픽_구독_테스트() throws Exception {
        // given
        List<Transport> transports = List.of(
                new WebSocketTransport(new StandardWebSocketClient()),
                new RestTemplateXhrTransport()
        );
        WebSocketClient sockJsClient = new SockJsClient(transports);
        WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        CountDownLatch connectionLatch = new CountDownLatch(1);
        String url = "http://localhost:" + port + "/ws";

        // when
        StompSession session = stompClient.connectAsync(url, new SimpleStompSessionHandler(connectionLatch))
                .get(10, TimeUnit.SECONDS);

        // 방 토픽 구독
        session.subscribe("/topic/room/1", new TestFrameHandler());

        // then
        assertThat(connectionLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(session.isConnected()).isTrue();

        session.disconnect();
    }

    @Test
    void 플레이어_목록_요청_메시지_전송_테스트() throws Exception {
        // given
        List<Transport> transports = List.of(
                new WebSocketTransport(new StandardWebSocketClient()),
                new RestTemplateXhrTransport()
        );
        WebSocketClient sockJsClient = new SockJsClient(transports);
        WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        CountDownLatch connectionLatch = new CountDownLatch(1);
        String url = "http://localhost:" + port + "/ws";

        // when
        StompSession session = stompClient.connectAsync(url, new SimpleStompSessionHandler(connectionLatch))
                .get(10, TimeUnit.SECONDS);

        // 토픽 구독
        session.subscribe("/topic/room/1", new TestFrameHandler());

        // 메시지 전송 (실제 데이터 없어도 전송 자체는 가능)
        session.send("/app/room/1/players", null);

        // then - 메시지 전송이 완료되었는지 확인
        Thread.sleep(1000); // 비동기 처리 대기
        assertThat(session.isConnected()).isTrue();

        session.disconnect();
    }

    @Test
    void 여러_토픽_동시_구독_테스트() throws Exception {
        // given
        List<Transport> transports = List.of(
                new WebSocketTransport(new StandardWebSocketClient()),
                new RestTemplateXhrTransport()
        );
        WebSocketClient sockJsClient = new SockJsClient(transports);
        WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        CountDownLatch connectionLatch = new CountDownLatch(1);
        String url = "http://localhost:" + port + "/ws";

        // when
        StompSession session = stompClient.connectAsync(url, new SimpleStompSessionHandler(connectionLatch))
                .get(10, TimeUnit.SECONDS);

        // 여러 토픽 구독
        session.subscribe("/topic/room/1", new TestFrameHandler());
        session.subscribe("/topic/room/1/roulette", new TestFrameHandler());
        session.subscribe("/topic/room/1/minigame", new TestFrameHandler());

        // then
        assertThat(connectionLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(session.isConnected()).isTrue();

        session.disconnect();
    }

    private static class SimpleStompSessionHandler implements StompSessionHandler {
        private final CountDownLatch latch;

        public SimpleStompSessionHandler(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            System.out.println("✅ SockJS 웹소켓 연결 성공!");
            latch.countDown();
        }

        @Override
        public void handleException(StompSession session, StompCommand command,
                                    StompHeaders headers, byte[] payload, Throwable exception) {
            System.err.println("❌ STOMP 에러: " + exception.getMessage());
            exception.printStackTrace();
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            System.err.println("❌ 전송 에러: " + exception.getMessage());
            exception.printStackTrace();
        }

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return String.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            System.out.println("📨 프레임 수신: " + payload);
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
