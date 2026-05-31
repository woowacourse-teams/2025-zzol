package coffeeshout.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.WebsocketModuleRandomPortTest;
import coffeeshout.support.TestStompSession;
import coffeeshout.support.TestStompSessionFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.simp.stomp.StompHeaders;

class WebSocketConnectionTest extends WebsocketModuleRandomPortTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void 구독_요청_후_세션이_활성_상태다() throws Exception {
        TestStompSession session = new TestStompSessionFactory(port, objectMapper).connect(new StompHeaders());
        try {
            session.subscribe("/topic/room/TEST");
            assertThat(session.isConnected()).isTrue();
        } finally {
            session.disconnect();
        }
    }
}
