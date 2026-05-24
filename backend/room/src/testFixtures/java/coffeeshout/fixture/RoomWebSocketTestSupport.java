package coffeeshout.fixture;

import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.infra.auth.RoomSessionTokenService;
import coffeeshout.support.TestStompSession;
import coffeeshout.support.WebSocketIntegrationTestSupport;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class RoomWebSocketTestSupport extends WebSocketIntegrationTestSupport {

    private static final String SMOKE_JOIN_CODE = "SMOK";
    private static final String SMOKE_PLAYER_NAME = "smoketest";

    @Autowired
    private RoomSessionTokenService roomSessionTokenService;

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
}
