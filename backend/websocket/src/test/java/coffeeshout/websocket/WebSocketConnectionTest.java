package coffeeshout.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.WebsocketModuleRandomPortTest;
import coffeeshout.support.TestStompSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

class WebSocketConnectionTest extends WebsocketModuleRandomPortTest {

    private static final int CONNECT_TIMEOUT_SECONDS = 1;
    private static final String WEBSOCKET_BASE_URL_FORMAT = "ws://localhost:%d/ws";

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void 연결_후_토픽을_구독할_수_있다() throws Exception {
        TestStompSession session = createSession();

        session.subscribe("/topic/room/TEST");

        assertThat(session.isConnected()).isTrue();

        session.disconnect();
    }

    private TestStompSession createSession() throws InterruptedException, ExecutionException, TimeoutException {
        final SockJsClient sockJsClient = new SockJsClient(
                List.of(new WebSocketTransport(new StandardWebSocketClient())));
        final WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
        final MappingJackson2MessageConverter messageConverter = new MappingJackson2MessageConverter(objectMapper);
        messageConverter.setStrictContentTypeMatch(false);
        stompClient.setMessageConverter(messageConverter);

        final CompletableFuture<String> principalFuture = new CompletableFuture<>();
        final StompSession session = stompClient
                .connectAsync(
                        String.format(WEBSOCKET_BASE_URL_FORMAT, port),
                        new WebSocketHttpHeaders(),
                        new StompHeaders(),
                        new StompSessionHandlerAdapter() {
                            @Override
                            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                                principalFuture.complete(connectedHeaders.getFirst("user-name"));
                            }
                        })
                .get(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        final TestStompSession testSession = new TestStompSession(session, objectMapper);
        testSession.setPrincipalName(principalFuture.get(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        return testSession;
    }
}
