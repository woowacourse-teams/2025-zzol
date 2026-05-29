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
    void 연결_후_토픽을_구독할_수_있다() throws Exception {
        TestStompSession session = new TestStompSessionFactory(port, objectMapper).connect(new StompHeaders());

        session.subscribe("/topic/room/TEST");

        assertThat(session.isConnected()).isTrue();

        session.disconnect();
    }
}
