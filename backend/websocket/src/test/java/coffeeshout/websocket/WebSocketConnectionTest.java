package coffeeshout.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.WebsocketModuleWebSocketIntegrationTest;
import coffeeshout.support.TestStompSession;
import org.junit.jupiter.api.Test;

class WebSocketConnectionTest extends WebsocketModuleWebSocketIntegrationTest {

    @Test
    void 구독_요청_후_세션이_활성_상태다() throws Exception {
        try (TestStompSession session = createSessionWithoutRoomToken()) {
            session.subscribe("/topic/room/TEST");
            assertThat(session.isConnected()).isTrue();
        }
    }
}
