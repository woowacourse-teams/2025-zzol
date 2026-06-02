package coffeeshout.report.application;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.AdminModuleServiceTest;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.report.infra.persistence.Report;
import coffeeshout.report.domain.ReportCategory;
import coffeeshout.report.infra.persistence.ReportRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ReportServiceTest extends AdminModuleServiceTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private ReportRepository reportRepository;

    @Nested
    @DisplayName("submit")
    class Submit {

        @Test
        void BUG_신고를_저장하고_id를_반환한다() {
            final long reportId = reportService.submit(
                    ReportCategory.BUG, MiniGameType.CARD_GAME, "ABC12", "카드게임이 멈춰요."
            );

            final Report saved = reportRepository.findById(reportId).orElseThrow();
            assertThat(saved.getCategory()).isEqualTo(ReportCategory.BUG);
            assertThat(saved.getGameType()).isEqualTo(MiniGameType.CARD_GAME);
            assertThat(saved.getJoinCode()).isEqualTo("ABC12");
        }

        @Test
        void 건의사항을_저장하고_id를_반환한다() {
            final long reportId = reportService.submit(
                    ReportCategory.SUGGESTION, null, null, "새 게임 추가해주세요."
            );

            final Report saved = reportRepository.findById(reportId).orElseThrow();
            assertThat(saved.getCategory()).isEqualTo(ReportCategory.SUGGESTION);
            assertThat(saved.getGameType()).isNull();
            assertThat(saved.getJoinCode()).isNull();
        }
    }
}
