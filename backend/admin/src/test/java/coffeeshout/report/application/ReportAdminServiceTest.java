package coffeeshout.report.application;

import static org.assertj.core.api.Assertions.assertThat;
import static coffeeshout.support.ExceptionAssertions.assertCoffeeShoutException;

import coffeeshout.AdminModuleServiceTest;
import coffeeshout.global.exception.GlobalErrorCode;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.report.domain.ReportCategory;
import coffeeshout.report.infra.persistence.Report;
import coffeeshout.report.infra.persistence.ReportRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("ReportAdminService")
class ReportAdminServiceTest extends AdminModuleServiceTest {

    @Autowired
    private ReportAdminService reportAdminService;

    @Autowired
    private ReportRepository reportRepository;

    @Nested
    @DisplayName("findReporterIp")
    class FindReporterIp {

        @Test
        void ip가_저장된_신고의_ip를_반환한다() {
            final Report saved = reportRepository.save(
                    Report.createBugReport(MiniGameType.CARD_GAME, "ABC12", "내용", java.time.Clock.systemUTC(),
                            null, "1.2.3.4")
            );

            final String ip = reportAdminService.findReporterIp(saved.getId());

            assertThat(ip).isEqualTo("1.2.3.4");
        }

        @Test
        void ip_없이_제출된_신고는_null을_반환한다() {
            final Report saved = reportRepository.save(
                    Report.createGeneralReport(ReportCategory.SUGGESTION, "내용", java.time.Clock.systemUTC())
            );

            final String ip = reportAdminService.findReporterIp(saved.getId());

            assertThat(ip).isNull();
        }

        @Test
        void 존재하지_않는_신고_id면_예외를_던진다() {
            assertCoffeeShoutException(
                    () -> reportAdminService.findReporterIp(9999L),
                    GlobalErrorCode.NOT_EXIST
            );
        }
    }
}
