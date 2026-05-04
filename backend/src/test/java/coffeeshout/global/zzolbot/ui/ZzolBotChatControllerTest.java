package coffeeshout.global.zzolbot.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import coffeeshout.global.zzolbot.application.ZzolBotChatService;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class ZzolBotChatControllerTest {

    @Mock
    private ZzolBotChatService chatService;

    private ZzolBotChatController controller;

    @BeforeEach
    void setUp() {
        controller = new ZzolBotChatController(chatService);
    }

    @Nested
    class page_메서드 {

        @Test
        void zzolbot_뷰_이름을_반환한다() {
            assertThat(controller.page()).isEqualTo("admin/zzolbot");
        }
    }

    @Nested
    class ask_메서드 {

        @Test
        void SseEmitter를_반환하고_chatService를_비동기로_호출한다() {
            given(chatService.ask(anyString(), any())).willReturn("방 A4BX는 PLAYING 상태입니다.");

            final SseEmitter emitter = controller.ask(new ZzolBotChatController.AskRequest("A4BX 방 상태"));

            assertThat(emitter).isNotNull();
            await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
                    verify(chatService).ask(eq("A4BX 방 상태"), any())
            );
        }
    }
}
