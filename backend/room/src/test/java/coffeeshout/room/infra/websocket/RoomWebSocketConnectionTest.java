package coffeeshout.room.infra.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.RoomModuleWebSocketTest;
import coffeeshout.support.TestStompSession;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.stomp.ConnectionLostException;

class RoomWebSocketConnectionTest extends RoomModuleWebSocketTest {

    @Test
    void 유효한_RST로_연결하면_성공한다() throws Exception {
        TestStompSession session = createSession();

        assertThat(session.isConnected()).isTrue();

        session.disconnect();
    }

    @Test
    void 위조된_RST로_연결하면_거부된다() {
        assertThatThrownBy(() -> createSessionWithRoomToken("invalid.token.value"))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(ConnectionLostException.class);
    }
}
