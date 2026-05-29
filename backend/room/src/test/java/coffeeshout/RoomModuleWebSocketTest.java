package coffeeshout;

import coffeeshout.config.ServiceTestConfig;
import coffeeshout.room.infra.auth.RoomSessionTokenService;
import coffeeshout.support.IntegrationTestSupport;
import coffeeshout.support.TestStompSession;
import coffeeshout.support.TestStompSessionFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.stomp.StompHeaders;

@SpringBootTest(classes = RoomModuleTestApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(ServiceTestConfig.class)
public abstract class RoomModuleWebSocketTest extends IntegrationTestSupport {

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

    protected TestStompSession createSessionWithRoomToken(String roomToken)
            throws InterruptedException, ExecutionException, TimeoutException {
        final StompHeaders headers = new StompHeaders();
        headers.add("roomToken", roomToken);
        return stompFactory.connect(headers);
    }
}
