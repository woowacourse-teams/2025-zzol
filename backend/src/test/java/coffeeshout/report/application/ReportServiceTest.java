package coffeeshout.report.application;

import static coffeeshout.global.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.global.ServiceTest;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.report.domain.ReportCategory;
import coffeeshout.report.exception.ReportErrorCode;
import coffeeshout.report.infra.persistence.ReportEntity;
import coffeeshout.report.domain.repository.ReportRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ReportServiceTest extends ServiceTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private ReportRepository reportRepository;

    private static final String IP = "1.2.3.4";
    private static final String ANOTHER_IP = "5.6.7.8";

    @Nested
    @DisplayName("submit")
    class Submit {

        @Test
        void BUG_신고를_저장하고_id를_반환한다() {
            final long reportId = reportService.submit(
                    IP, ReportCategory.BUG, MiniGameType.CARD_GAME, "ABC12", "카드게임이 멈춰요."
            );

            final ReportEntity saved = reportRepository.findById(reportId).orElseThrow();
            assertThat(saved.getCategory()).isEqualTo(ReportCategory.BUG);
            assertThat(saved.getGameType()).isEqualTo(MiniGameType.CARD_GAME);
            assertThat(saved.getJoinCode()).isEqualTo("ABC12");
        }

        @Test
        void 건의사항을_저장하고_id를_반환한다() {
            final long reportId = reportService.submit(
                    IP, ReportCategory.SUGGESTION, null, null, "새 게임 추가해주세요."
            );

            final ReportEntity saved = reportRepository.findById(reportId).orElseThrow();
            assertThat(saved.getCategory()).isEqualTo(ReportCategory.SUGGESTION);
            assertThat(saved.getGameType()).isNull();
            assertThat(saved.getJoinCode()).isNull();
        }
    }

    @Nested
    @DisplayName("submit - rate limit")
    class SubmitRateLimit {

        @Test
        void 동일_IP에서_임계값_이내_요청은_모두_성공한다() {
            for (int i = 0; i < 5; i++) {
                final int seq = i;
                assertThat(reportService.submit(IP, ReportCategory.SUGGESTION, null, null, "내용" + seq))
                        .isPositive();
            }
        }

        @Test
        void 동일_IP에서_임계값_초과_요청은_예외를_던진다() {
            for (int i = 0; i < 5; i++) {
                reportService.submit(IP, ReportCategory.SUGGESTION, null, null, "내용" + i);
            }

            assertCoffeeShoutException(
                    () -> reportService.submit(IP, ReportCategory.SUGGESTION, null, null, "초과 요청"),
                    ReportErrorCode.REPORT_RATE_LIMITED
            );
        }

        @Test
        void 다른_IP는_rate_limit이_독립적으로_적용된다() {
            for (int i = 0; i < 5; i++) {
                reportService.submit(IP, ReportCategory.SUGGESTION, null, null, "내용" + i);
            }

            assertThat(reportService.submit(ANOTHER_IP, ReportCategory.SUGGESTION, null, null, "다른 IP 요청"))
                    .isPositive();
        }
    }
}
