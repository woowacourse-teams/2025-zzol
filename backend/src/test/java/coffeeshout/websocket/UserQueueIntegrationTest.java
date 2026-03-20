package coffeeshout.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;

import coffeeshout.fixture.TestStompSession;
import coffeeshout.fixture.TestStompSession.MessageCollector;
import coffeeshout.fixture.WebSocketIntegrationTestSupport;
import coffeeshout.global.MessageResponse;
import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.room.ui.request.ReadyChangeMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class UserQueueIntegrationTest extends WebSocketIntegrationTestSupport {

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StreamPublisher streamPublisher;

    @Test
    void CoffeeShoutException_발생_시_ErrorCode_메시지를_수신한다() throws Exception {
        // given
        TestStompSession session = createSession("TEST_CODE", "testPlayer");
        MessageCollector errorCollector = session.subscribe("/user/queue/errors");

        // when - 존재하지 않는 방에 룰렛 스핀 요청 → NotExistElementException(GlobalErrorCode.NOT_EXIST)
        session.send("/app/room/ABCD/spin-roulette", "{\"hostName\": \"testPlayer\"}");

        // then - ErrorCode.getMessage()인 "해당 데이터가 존재하지 않습니다."가 반환되어야 함
        MessageResponse response = errorCollector.get();
        assertThat(response.payload()).contains("\"success\":false");
        assertThat(response.payload()).contains("해당 데이터가 존재하지 않습니다.");
    }

    @Test
    void 일반_Exception_발생_시_기본_에러_메시지를_수신한다() throws Exception {
        // given
        willThrow(new RuntimeException("예상치 못한 서버 오류"))
                .given(streamPublisher).publish(any(), any());

        TestStompSession session = createSession("TEST_CODE", "testPlayer");
        MessageCollector errorCollector = session.subscribe("/user/queue/errors");

        ReadyChangeMessage readyChangeMessage = new ReadyChangeMessage("ABCD", "testPlayer", true);
        String body = objectMapper.writeValueAsString(readyChangeMessage);

        // when - streamPublisher.publish()에서 RuntimeException 발생
        session.send("/app/room/ABCD/update-ready", body);

        // then - CoffeeShoutException이 아니므로 기본 에러 메시지가 반환되어야 함
        MessageResponse response = errorCollector.get();
        assertThat(response.payload()).contains("\"success\":false");
        assertThat(response.payload()).contains("처리 중 오류가 발생했습니다.");
    }
}
