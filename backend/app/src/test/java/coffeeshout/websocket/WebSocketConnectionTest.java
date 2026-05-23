package coffeeshout.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.support.TestStompSession;
import coffeeshout.fixture.WebSocketIntegrationTestSupport;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.stomp.ConnectionLostException;

class WebSocketConnectionTest extends WebSocketIntegrationTestSupport {

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

    @Test
    void 연결_후_토픽을_구독할_수_있다() throws Exception {
        TestStompSession session = createSession();

        session.subscribe("/topic/room/TEST");

        assertThat(session.isConnected()).isTrue();

        session.disconnect();
    }
}
