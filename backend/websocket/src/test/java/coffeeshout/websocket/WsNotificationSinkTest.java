package coffeeshout.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import coffeeshout.websocket.ui.WebSocketResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("WS 알림 전달 지점")
class WsNotificationSinkTest {

    private static final String DESTINATION = "/topic/room/ABC123/gameState";

    @Mock
    private LoggingSimpMessagingTemplate loggingSimpMessagingTemplate;

    private WsNotificationSink sink;

    @BeforeEach
    void setUp() {
        sink = new WsNotificationSink(loggingSimpMessagingTemplate, new ObjectMapper());
    }

    @Test
    @DisplayName("유효한 페이로드는 WebSocketResponse로 되살려 전송하고 true를 반환한다")
    void 유효한_페이로드는_WebSocketResponse로_되살려_전송한다() {
        // given
        final String payloadJson = "{\"success\":true,\"data\":{\"state\":\"PLAYING\"}}";

        // when
        final boolean delivered = sink.deliver(DESTINATION, payloadJson);

        // then
        final ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(loggingSimpMessagingTemplate).convertAndSend(eq(DESTINATION), payload.capture());
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(delivered).isTrue();
            // 복구 저장 조건: 문자열이 아니라 WebSocketResponse 타입으로 전달돼야 한다
            softly.assertThat(payload.getValue()).isInstanceOf(WebSocketResponse.class);
        });
    }

    @Test
    @DisplayName("역직렬화가 실패하면 전송 없이 false를 반환하고 예외를 던지지 않는다")
    void 역직렬화_실패는_전송_없이_false를_반환한다() {
        // when
        final boolean delivered = sink.deliver(DESTINATION, "not-json");

        // then
        assertThat(delivered).isFalse();
        verifyNoInteractions(loggingSimpMessagingTemplate);
    }
}
