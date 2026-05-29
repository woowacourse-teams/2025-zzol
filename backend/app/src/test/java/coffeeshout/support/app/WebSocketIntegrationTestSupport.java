package coffeeshout.support.app;

import static org.skyscreamer.jsonassert.JSONCompareMode.LENIENT;

import coffeeshout.support.MessageResponse;
import coffeeshout.support.TestStompSession;
import coffeeshout.support.TestStompSessionFactory;
import coffeeshout.room.infra.auth.RoomSessionTokenService;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.player.PlayerName;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private TestStompSessionFactory stompFactory;

    @BeforeEach
    void cleanupRedisListeners() {
        redisMessageListenerContainer.stop();
        redisMessageListenerContainer.start();
        stompFactory = new TestStompSessionFactory(port, objectMapper);
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
        return stompFactory.connect(new StompHeaders());
    }

    protected TestStompSession createSessionWithAuthorizationToken(String accessToken)
            throws InterruptedException, ExecutionException, TimeoutException {
        final StompHeaders headers = new StompHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        return stompFactory.connect(headers);
    }

    protected TestStompSession createSessionWithRoomToken(String roomToken)
            throws InterruptedException, ExecutionException, TimeoutException {
        final StompHeaders headers = new StompHeaders();
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
