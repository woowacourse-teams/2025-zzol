package coffeeshout;

import coffeeshout.config.ServiceTestConfig;
import coffeeshout.room.infra.auth.RoomSessionTokenService;
import coffeeshout.room.infra.websocket.StompPrincipalInterceptor;
import coffeeshout.support.TestStompSession;
import coffeeshout.support.WebSocketIntegrationTestSupport;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.simp.stomp.StompHeaders;

@SpringBootTest(classes = RoomModuleTestApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(ServiceTestConfig.class)
public abstract class RoomModuleWebSocketTest extends WebSocketIntegrationTestSupport {

    private static final String SMOKE_JOIN_CODE = "SMOK";
    private static final String SMOKE_PLAYER_NAME = "smoketest";

    @Autowired
    private RoomSessionTokenService roomSessionTokenService;

    protected TestStompSession createSession() throws InterruptedException, ExecutionException, TimeoutException {
        final String token = roomSessionTokenService.issue(SMOKE_JOIN_CODE, SMOKE_PLAYER_NAME, null);
        return createSessionWithRoomToken(token);
    }

    protected TestStompSession createSession(String joinCode, String playerName)
            throws InterruptedException, ExecutionException, TimeoutException {
        final String token = roomSessionTokenService.issue(joinCode, playerName, null);
        return createSessionWithRoomToken(token);
    }

    protected TestStompSession createSessionWithRoomToken(String roomToken)
            throws InterruptedException, ExecutionException, TimeoutException {
        final StompHeaders headers = new StompHeaders();
        headers.add(StompPrincipalInterceptor.ROOM_TOKEN_HEADER, roomToken);
        return connect(headers);
    }
}
