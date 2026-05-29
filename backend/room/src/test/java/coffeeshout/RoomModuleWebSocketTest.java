package coffeeshout;

import coffeeshout.config.ServiceTestConfig;
import coffeeshout.room.infra.auth.RoomSessionTokenService;
import coffeeshout.support.IntegrationTestSupport;
import coffeeshout.support.TestStompSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.ConnectionLostException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

@SpringBootTest(classes = RoomModuleTestApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(ServiceTestConfig.class)
public abstract class RoomModuleWebSocketTest extends IntegrationTestSupport {

    private static final int CONNECT_TIMEOUT_SECONDS = 1;
    private static final String WEBSOCKET_BASE_URL_FORMAT = "ws://localhost:%d/ws";
    private static final String SMOKE_JOIN_CODE = "SMOK";
    private static final String SMOKE_PLAYER_NAME = "smoketest";

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisMessageListenerContainer redisMessageListenerContainer;

    @Autowired
    private RoomSessionTokenService roomSessionTokenService;

    @BeforeEach
    void cleanupRedisListeners() {
        redisMessageListenerContainer.stop();
        redisMessageListenerContainer.start();
    }

    protected TestStompSession createSession() throws InterruptedException, ExecutionException, TimeoutException {
        final String token = roomSessionTokenService.issue(SMOKE_JOIN_CODE, SMOKE_PLAYER_NAME, null);
        return createSessionWithRoomToken(token);
    }

    protected TestStompSession createSessionWithRoomToken(String roomToken)
            throws InterruptedException, ExecutionException, TimeoutException {
        final SockJsClient sockJsClient = new SockJsClient(
                List.of(new WebSocketTransport(new StandardWebSocketClient())));
        final WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
        final MappingJackson2MessageConverter messageConverter = new MappingJackson2MessageConverter(objectMapper);
        messageConverter.setStrictContentTypeMatch(false);
        stompClient.setMessageConverter(messageConverter);

        final StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("roomToken", roomToken);

        final CompletableFuture<String> principalFuture = new CompletableFuture<>();
        final StompSession session = stompClient
                .connectAsync(
                        String.format(WEBSOCKET_BASE_URL_FORMAT, port),
                        new WebSocketHttpHeaders(),
                        connectHeaders,
                        new StompSessionHandlerAdapter() {
                            @Override
                            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                                principalFuture.complete(connectedHeaders.getFirst("user-name"));
                            }

                            @Override
                            public void handleTransportError(StompSession session, Throwable exception) {
                                if (exception instanceof ConnectionLostException) {
                                    principalFuture.completeExceptionally(exception);
                                    return;
                                }
                                principalFuture.completeExceptionally(exception);
                                throw new RuntimeException(exception);
                            }

                            @Override
                            public void handleException(StompSession session, StompCommand command,
                                    StompHeaders headers, byte[] payload, Throwable exception) {
                                principalFuture.completeExceptionally(exception);
                                throw new RuntimeException(exception);
                            }
                        })
                .get(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        final TestStompSession testSession = new TestStompSession(session, objectMapper);
        testSession.setPrincipalName(principalFuture.get(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        return testSession;
    }
}
