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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.stomp.StompHeaders;

public abstract class WebSocketIntegrationTestSupport extends IntegrationTestSupport {

    /**
     * 페이즈 전이 메시지의 도착 시간을 검증할 때 쓰는 하한 허용오차(ms).
     *
     * <p>도착 시간 검증은 "타이머가 실제 경과한 뒤 전이됐는가(조기 전이가 아닌가)"만 본다(하한). 상한은 두지 않는다 —
     * {@code MessageCollector.get()}이 Awaitility 폴링(최대 100ms)으로 대기하고 전체 스위트 부하로 도착이 늦어질 수
     * 있어, 상한 검증은 의미 신호 없이 flaky하기만 하다. 과도하게 늦는 경우는 {@code get(timeout)}의 대기 한도가 이미 거른다.
     */
    private static final long TIMING_LOWER_TOLERANCE_MS = 100;

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

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

    protected TestStompSession connect(StompHeaders headers)
            throws InterruptedException, ExecutionException, TimeoutException {
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
            softly.assertThat(response.duration()).isGreaterThanOrEqualTo(duration - TIMING_LOWER_TOLERANCE_MS);
        });
    }

    protected void assertMessageContains(MessageResponse response, String expected) {
        SoftAssertions.assertSoftly(softly -> softly.assertThat(response.payload()).contains(expected));
    }

    protected void assertMessage(MessageResponse response, long duration, String payload) throws JSONException {
        JSONAssert.assertEquals(payload, response.payload(), false);
        Assertions.assertThat(response.duration()).isGreaterThanOrEqualTo(duration - TIMING_LOWER_TOLERANCE_MS);
    }
}
