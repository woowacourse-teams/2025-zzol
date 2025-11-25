package coffeeshout.fixture;

import static org.skyscreamer.jsonassert.JSONCompareMode.LENIENT;

import coffeeshout.global.MessageResponse;
import coffeeshout.global.config.IntegrationTestConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({IntegrationTestConfig.class})
@Transactional
public abstract class WebSocketIntegrationTestSupport {

    static final int CONNECT_TIMEOUT_SECONDS = 1;
    static final String WEBSOCKET_BASE_URL_FORMAT = "ws://localhost:%d/ws";
    private static final Logger log = LoggerFactory.getLogger(WebSocketIntegrationTestSupport.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisMessageListenerContainer redisMessageListenerContainer;

    @LocalServerPort
    private int port;

    @BeforeEach
    void cleanupRedisListeners() {
        // 모든 리스너 제거 후 재시작하여 중복 등록 방지
        redisMessageListenerContainer.stop();
        redisMessageListenerContainer.start();
    }

    protected TestStompSession createSession() throws InterruptedException, ExecutionException, TimeoutException {
        SockJsClient sockJsClient = new SockJsClient(List.of(
                new WebSocketTransport(new StandardWebSocketClient())
        ));

        WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
        MappingJackson2MessageConverter messageConverter = new MappingJackson2MessageConverter(objectMapper);
        messageConverter.setStrictContentTypeMatch(false);
        stompClient.setMessageConverter(messageConverter);
        String url = String.format(WEBSOCKET_BASE_URL_FORMAT, port);
        StompSession session = stompClient
                .connectAsync(
                        url, new StompSessionHandlerAdapter() {

                            @Override
                            public void handleTransportError(StompSession session, Throwable exception) {
                                log.error("STOMP TRANSPORT ERROR: " + exception.getMessage());
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
                                throw new RuntimeException(exception);
                            }
                        }
                )
                .get(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        return new TestStompSession(session, objectMapper);
    }

    protected void assertMessage(MessageResponse response, String payload) throws JSONException {
        JSONAssert.assertEquals(response.payload(), payload, false);
    }

    protected void assertMessageCustomization(
            MessageResponse response,
            String payload,
            Customization customization
    ) throws JSONException {
        JSONAssert.assertEquals(
                response.payload(),
                payload,
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
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.payload()).contains(expected);
        });
    }

    protected void assertMessage(MessageResponse response, long duration, String payload) throws JSONException {
        JSONAssert.assertEquals(response.payload(), payload, false);
        Assertions.assertThat(response.duration()).isBetween(duration - 100, duration + 100);
    }
}
