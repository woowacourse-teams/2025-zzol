package coffeeshout.support.app;

import static org.skyscreamer.jsonassert.JSONCompareMode.LENIENT;

import coffeeshout.support.MessageResponse;
import coffeeshout.support.TestStompSession;
import coffeeshout.room.infra.auth.RoomSessionTokenService;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.player.PlayerName;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
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

public abstract class WebSocketIntegrationTestSupport extends IntegrationTestSupport {

    static final int CONNECT_TIMEOUT_SECONDS = 1;
    static final String WEBSOCKET_BASE_URL_FORMAT = "ws://localhost:%d/ws";
    private static final Logger log = LoggerFactory.getLogger(WebSocketIntegrationTestSupport.class);
    private static final String SMOKE_JOIN_CODE = "SMOK";
    private static final String SMOKE_PLAYER_NAME = "smoketest";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisMessageListenerContainer redisMessageListenerContainer;

    @Autowired
    private RoomSessionTokenService roomSessionTokenService;

    @LocalServerPort
    private int port;

    @BeforeEach
    void cleanupRedisListeners() {
        // 모든 리스너 제거 후 재시작하여 중복 등록 방지
        redisMessageListenerContainer.stop();
        redisMessageListenerContainer.start();
    }

    protected TestStompSession createSession() throws InterruptedException, ExecutionException, TimeoutException {
        final String token = roomSessionTokenService.issue(SMOKE_JOIN_CODE, SMOKE_PLAYER_NAME, null);
        return createSessionWithRoomToken(token);
    }

    protected TestStompSession createSession(JoinCode joinCode, PlayerName playerName)
            throws InterruptedException, ExecutionException, TimeoutException {
        return createSession(joinCode.getValue(), playerName.value());
    }

    protected TestStompSession createSession(String joinCode, String playerName)
            throws InterruptedException, ExecutionException, TimeoutException {
        final String token = roomSessionTokenService.issue(joinCode, playerName, null);
        return createSessionWithRoomToken(token);
    }

    protected TestStompSession createSessionWithoutRoomToken()
            throws InterruptedException, ExecutionException, TimeoutException {
        return createSessionWithConnectHeaders(new StompHeaders());
    }

    protected TestStompSession createSessionWithAuthorizationToken(String accessToken)
            throws InterruptedException, ExecutionException, TimeoutException {
        final StompHeaders headers = new StompHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        return createSessionWithConnectHeaders(headers);
    }

    private TestStompSession createSessionWithConnectHeaders(StompHeaders connectHeaders)
            throws InterruptedException, ExecutionException, TimeoutException {
        final SockJsClient sockJsClient = new SockJsClient(List.of(
                new WebSocketTransport(new StandardWebSocketClient())
        ));
        final WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
        final MappingJackson2MessageConverter messageConverter = new MappingJackson2MessageConverter(objectMapper);
        messageConverter.setStrictContentTypeMatch(false);
        stompClient.setMessageConverter(messageConverter);

        // CompletableFuture로 principal 수신 — Spring의 DefaultStompSession은
        // sessionFuture.complete() 후 afterConnected()를 호출하므로 String[]이면
        // 테스트 스레드가 .get() 반환 시 principalHolder가 아직 null일 수 있다.
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
                        }
                )
                .get(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        final TestStompSession testSession = new TestStompSession(session, objectMapper);
        testSession.setPrincipalName(principalFuture.get(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        return testSession;
    }

    protected TestStompSession createSessionWithRoomToken(String roomToken)
            throws InterruptedException, ExecutionException, TimeoutException {
        final SockJsClient sockJsClient = new SockJsClient(List.of(
                new WebSocketTransport(new StandardWebSocketClient())
        ));

        final WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
        final MappingJackson2MessageConverter messageConverter = new MappingJackson2MessageConverter(objectMapper);
        messageConverter.setStrictContentTypeMatch(false);
        stompClient.setMessageConverter(messageConverter);
        final String url = String.format(WEBSOCKET_BASE_URL_FORMAT, port);

        final StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("roomToken", roomToken);

        final CompletableFuture<String> principalFuture = new CompletableFuture<>();
        final StompSession session = stompClient
                .connectAsync(
                        url, new WebSocketHttpHeaders(), connectHeaders, new StompSessionHandlerAdapter() {

                            @Override
                            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                                principalFuture.complete(connectedHeaders.getFirst("user-name"));
                            }

                            @Override
                            public void handleTransportError(StompSession session, Throwable exception) {
                                if (exception instanceof ConnectionLostException) {
                                    log.debug("STOMP connection closed");
                                    principalFuture.completeExceptionally(exception);
                                    return;
                                }
                                log.error("STOMP TRANSPORT ERROR: {}", exception.getMessage());
                                principalFuture.completeExceptionally(exception);
                                throw new RuntimeException(exception);
                            }

                            @Override
                            public void handleException(
                                    StompSession session,
                                    StompCommand command,
                                    StompHeaders headers,
                                    byte[] payload,
                                    Throwable exception
                            ) {
                                log.error("STOMP EXCEPTION: " + exception.getMessage());
                                principalFuture.completeExceptionally(exception);
                                throw new RuntimeException(exception);
                            }
                        }
                )
                .get(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        final TestStompSession testSession = new TestStompSession(session, objectMapper);
        testSession.setPrincipalName(principalFuture.get(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        return testSession;
    }

    protected void assertMessage(MessageResponse response, String payload) throws JSONException {
        JSONAssert.assertEquals(payload, response.payload(), false);
    }

    protected void assertMessageCustomization(
            MessageResponse response,
            String payload,
            Customization customization
    ) throws JSONException {
        JSONAssert.assertEquals(
                payload,
                response.payload(),
                new CustomComparator(LENIENT, customization)
        );
    }

    protected void assertMessageContains(MessageResponse response, long duration, String expected) {
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.payload()).contains(expected);
            softly.assertThat(response.duration()).isBetween(duration - 100, duration + 100);
        });
    }

    protected void assertMessageContains(MessageResponse response, String expected) {
        SoftAssertions.assertSoftly(softly -> softly.assertThat(response.payload()).contains(expected));
    }

    protected void assertMessage(MessageResponse response, long duration, String payload) throws JSONException {
        JSONAssert.assertEquals(payload, response.payload(), false);
        Assertions.assertThat(response.duration()).isBetween(duration - 100, duration + 100);
    }
}
