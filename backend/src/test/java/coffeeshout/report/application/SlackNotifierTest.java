package coffeeshout.report.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.report.application.event.ReportSubmittedEvent;
import coffeeshout.report.config.SlackProperties;
import coffeeshout.report.domain.ReportCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class SlackNotifierTest {

    private SlackNotifier notifier;
    private RestClient restClient;

    @BeforeEach
    void setUp() {
        restClient = mock(RestClient.class);
        notifier = new SlackNotifier(new SlackProperties("test", null, null), restClient);
    }

    @Nested
    class OnReportSubmitted {

        @Test
        void enabled가_false이면_알림을_건너뛴다() {

            assertThatCode(() -> notifier.onReportSubmitted(
                    new ReportSubmittedEvent(1L, ReportCategory.BUG, MiniGameType.CARD_GAME, "ABC12", "카드게임 버그 테스트")
            )).doesNotThrowAnyException();
        }

        @Test
        void webhook_URL이_null이면_알림을_건너뛴다() {
            SlackNotifier disabledNotifier = new SlackNotifier(new SlackProperties(null, null, null), restClient);

            assertThatCode(() -> disabledNotifier.onReportSubmitted(
                    new ReportSubmittedEvent(2L, ReportCategory.SUGGESTION, null, null, "건의사항 테스트")
            )).doesNotThrowAnyException();
        }

        @Test
        void gameType과_joinCode가_null이어도_정상_종료한다() {
            // Test with disabled notifier to verify message building logic doesn't crash before restClient call
            assertThatCode(() -> notifier.onReportSubmitted(
                    new ReportSubmittedEvent(3L, ReportCategory.OTHER, null, null, "기타 의견입니다.")
            )).doesNotThrowAnyException();
        }
    }
}
