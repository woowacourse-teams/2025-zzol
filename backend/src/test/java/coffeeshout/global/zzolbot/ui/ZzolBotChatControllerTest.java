package coffeeshout.global.zzolbot.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import coffeeshout.global.zzolbot.application.ZzolBotChatService;
import coffeeshout.global.zzolbot.domain.ZzolBotChatResult;
import java.util.concurrent.Executors;
import coffeeshout.global.zzolbot.domain.ZzolBotFeedback;
import coffeeshout.global.zzolbot.infra.ZzolBotSessionEntity;
import java.security.Principal;
import java.time.Duration;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ZzolBotChatControllerTest {

    @Mock
    private ZzolBotChatService chatService;

    @Mock
    private Principal principal;

    private ZzolBotChatController controller;

    @BeforeEach
    void setUp() {
        controller = new ZzolBotChatController(chatService, Executors.newVirtualThreadPerTaskExecutor());
        given(principal.getName()).willReturn("admin");
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
            given(chatService.ask(anyString(), anyString(), any()))
                    .willReturn(new ZzolBotChatResult(1L, "방 A4BX는 PLAYING 상태입니다."));

            final SseEmitter emitter = controller.ask(
                    new ZzolBotChatController.AskRequest("A4BX 방 상태"),
                    principal
            );

            assertThat(emitter).isNotNull();
            await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
                    verify(chatService).ask(eq("A4BX 방 상태"), eq("admin"), any())
            );
        }
    }

    @Nested
    class feedback_메서드 {

        @Test
        void 피드백을_적용하고_200을_반환한다() {
            final ResponseEntity<Void> response = controller.feedback(
                    1L, new ZzolBotChatController.FeedbackRequest(ZzolBotFeedback.GOOD)
            );

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getStatusCode().value()).isEqualTo(200);
                softly.assertThatCode(() -> verify(chatService).applyFeedback(1L, ZzolBotFeedback.GOOD))
                        .doesNotThrowAnyException();
            });
        }
    }

    @Nested
    class sessions_메서드 {

        @Test
        void 최근_세션_목록을_SessionResponse로_변환해_반환한다() {
            final ZzolBotSessionEntity session = ZzolBotSessionEntity.create(
                    "A4BX 방 상태 알려줘", "PLAYING 상태입니다.", "admin"
            );
            given(chatService.getRecentSessions()).willReturn(List.of(session));

            final List<ZzolBotChatController.SessionResponse> result = controller.sessions();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result).hasSize(1);
                softly.assertThat(result.get(0).question()).isEqualTo("A4BX 방 상태 알려줘");
                softly.assertThat(result.get(0).answer()).isEqualTo("PLAYING 상태입니다.");
                softly.assertThat(result.get(0).feedback()).isNull();
            });
        }
    }
}
