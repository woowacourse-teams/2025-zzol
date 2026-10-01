package coffeeshout.websocket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import coffeeshout.websocket.ui.WebSocketResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class LoggingSimpMessagingTemplateTest {

    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    @Mock
    private WsRecoveryService wsRecoveryService;

    @InjectMocks
    private LoggingSimpMessagingTemplate template;

    @Test
    void transient_전송은_방_토픽이어도_복구_저장을_거치지_않는다() {
        // given — 고빈도 게임 델타는 복구 버퍼를 오염시키므로 저장 없이 보낸다
        final String destination = "/topic/room/ABCD/worm";
        final WebSocketResponse<String> payload = WebSocketResponse.success("delta");

        // when
        template.convertAndSendTransient(destination, payload);

        // then
        then(wsRecoveryService).should(never()).save(anyString(), anyString(), any());
        then(simpMessagingTemplate).should().convertAndSend(eq(destination), any(WebSocketResponse.class));
    }
}
