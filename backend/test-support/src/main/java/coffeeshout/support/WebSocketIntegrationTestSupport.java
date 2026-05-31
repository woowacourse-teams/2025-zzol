package coffeeshout.support;

import static org.skyscreamer.jsonassert.JSONCompareMode.LENIENT;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.stomp.StompHeaders;

public abstract class WebSocketIntegrationTestSupport extends IntegrationTestSupport {

    @LocalServerPort
    private int port;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Autowired(required = false)
    private RedisMessageListenerContainer redisMessageListenerContainer;

    private TestStompSessionFactory stompFactory;

    @BeforeEach
    void setupWebSocket() {
        if (redisMessageListenerContainer != null) {
            redisMessageListenerContainer.stop();
            redisMessageListenerContainer.start();
        }
        stompFactory = new TestStompSessionFactory(port, objectMapper);
    }

    protected TestStompSession createSessionWithoutRoomToken()
            throws InterruptedException, ExecutionException, TimeoutException {
        return stompFactory.connect(new StompHeaders());
    }

    protected TestStompSession createSessionWithAuthorizationToken(String accessToken)
            throws InterruptedException, ExecutionException, TimeoutException {
        StompHeaders headers = new StompHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        return stompFactory.connect(headers);
    }

    protected TestStompSession createSessionWithRoomToken(String roomToken)
            throws InterruptedException, ExecutionException, TimeoutException {
        StompHeaders headers = new StompHeaders();
        headers.add("roomToken", roomToken);
        return stompFactory.connect(headers);
    }

    protected void assertMessage(MessageResponse response, String payload) throws JSONException {
        JSONAssert.assertEquals(payload, response.payload(), false);
    }

    protected void assertMessageCustomization(
            MessageResponse response,
            String payload,
            Customization customization
    ) throws JSONException {
        JSONAssert.assertEquals(payload, response.payload(), new CustomComparator(LENIENT, customization));
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
