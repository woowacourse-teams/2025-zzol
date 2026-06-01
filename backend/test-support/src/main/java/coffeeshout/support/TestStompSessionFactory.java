package coffeeshout.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

public class TestStompSessionFactory {

    private static final int CONNECT_TIMEOUT_SECONDS = 3;
    private static final String URL_FORMAT = "ws://localhost:%d/ws";

    private final int port;
    private final ObjectMapper objectMapper;

    public TestStompSessionFactory(int port, ObjectMapper objectMapper) {
        this.port = port;
        this.objectMapper = objectMapper;
    }

    public TestStompSession connect(StompHeaders connectHeaders)
            throws InterruptedException, ExecutionException, TimeoutException {
        final SockJsClient sockJsClient = new SockJsClient(
                List.of(new WebSocketTransport(new StandardWebSocketClient())));
        final WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
        final MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter(objectMapper);
        converter.setStrictContentTypeMatch(false);
        stompClient.setMessageConverter(converter);

        final CompletableFuture<String> principalFuture = new CompletableFuture<>();
        final StompSession session = stompClient
                .connectAsync(
                        String.format(URL_FORMAT, port),
                        new WebSocketHttpHeaders(),
                        connectHeaders,
                        new StompSessionHandlerAdapter() {
                            @Override
                            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                                principalFuture.complete(connectedHeaders.getFirst("user-name"));
                            }

                            @Override
                            public void handleTransportError(StompSession session, Throwable exception) {
                                principalFuture.completeExceptionally(exception);
                            }

                            @Override
                            public void handleException(StompSession session, StompCommand command,
                                    StompHeaders headers, byte[] payload, Throwable exception) {
                                principalFuture.completeExceptionally(exception);
                                throw new RuntimeException(exception);
                            }
                        })
                .get(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        final TestStompSession testSession = new TestStompSession(session, stompClient, objectMapper);
        testSession.setPrincipalName(principalFuture.get(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        return testSession;
    }
}
