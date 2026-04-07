package coffeeshout.report.application;

import static org.assertj.core.api.Assertions.assertThatCode;

import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.report.application.event.ReportSubmittedEvent;
import coffeeshout.report.config.SlackProperties;
import coffeeshout.report.domain.ReportCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SlackNotifierTest {

    @Nested
    @DisplayName("onReportSubmitted")
    class OnReportSubmitted {

        @Test
        @DisplayName("webhook URL이 null이면 HTTP 호출 없이 정상 종료한다")
        void webhook_URL이_null이면_알림을_건너뛴다() {
            SlackNotifier notifier = new SlackNotifier(new SlackProperties(null));

            assertThatCode(() -> notifier.onReportSubmitted(
                    new ReportSubmittedEvent(1L, ReportCategory.BUG, MiniGameType.CARD_GAME, "ABC12", "카드게임 버그 테스트")
            )).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("webhook URL이 빈 문자열이면 HTTP 호출 없이 정상 종료한다")
        void webhook_URL이_빈_문자열이면_알림을_건너뛴다() {
            SlackNotifier notifier = new SlackNotifier(new SlackProperties(""));

            assertThatCode(() -> notifier.onReportSubmitted(
                    new ReportSubmittedEvent(2L, ReportCategory.SUGGESTION, null, null, "건의사항 테스트")
            )).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("gameType·joinCode가 null인 이벤트도 정상 처리한다")
        void gameType과_joinCode가_null이어도_정상_종료한다() {
            SlackNotifier notifier = new SlackNotifier(new SlackProperties(""));

            assertThatCode(() -> notifier.onReportSubmitted(
                    new ReportSubmittedEvent(3L, ReportCategory.OTHER, null, null, "기타 의견입니다.")
            )).doesNotThrowAnyException();
        }
    }
}
