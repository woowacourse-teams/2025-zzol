package coffeeshout.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.WebsocketModuleWebSocketIntegrationTest;
import coffeeshout.support.MessageResponse;
import coffeeshout.support.TestStompSession;
import coffeeshout.support.TestStompSession.MessageCollector;
import org.junit.jupiter.api.Test;

class UserQueueIntegrationTest extends WebsocketModuleWebSocketIntegrationTest {

    @Test
    void CoffeeShoutException_발생_시_ErrorCode_메시지를_수신한다() throws Exception {
        TestStompSession session = createSessionWithoutRoomToken();
        MessageCollector errorCollector = session.subscribe("/user/queue/errors");

        session.send("/app/test/throw-business-error", "{}");

        MessageResponse response = errorCollector.get();
        assertThat(response.payload()).contains("\"success\":false");
        assertThat(response.payload()).contains("해당 데이터가 존재하지 않습니다.");
    }

    @Test
    void 일반_Exception_발생_시_기본_에러_메시지를_수신한다() throws Exception {
        TestStompSession session = createSessionWithoutRoomToken();
        MessageCollector errorCollector = session.subscribe("/user/queue/errors");

        session.send("/app/test/throw-runtime-error", "{}");

        MessageResponse response = errorCollector.get();
        assertThat(response.payload()).contains("\"success\":false");
        assertThat(response.payload()).contains("처리 중 오류가 발생했습니다.");
    }
}
