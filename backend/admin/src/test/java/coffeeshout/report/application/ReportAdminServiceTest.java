package coffeeshout.report.application;

import static org.assertj.core.api.Assertions.assertThat;
import static coffeeshout.support.ExceptionAssertions.assertCoffeeShoutException;

import static org.assertj.core.api.Assertions.assertThatCode;

import coffeeshout.AdminModuleServiceTest;
import coffeeshout.global.exception.GlobalErrorCode;
import coffeeshout.global.ipblock.Ip;
import coffeeshout.global.ipblock.IpBlockStore;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.report.domain.ReportCategory;
import coffeeshout.report.infra.persistence.Report;
import coffeeshout.report.infra.persistence.Report.ReportCreation;
import coffeeshout.report.infra.persistence.ReportRepository;
import java.time.Clock;
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
                    Report.create(ReportCreation.bug(MiniGameType.CARD_GAME, "ABC12", "내용", null, "1.2.3.4"),
                            Clock.systemUTC())
            );

            final String ip = reportAdminService.findReporterIp(saved.getId());

            assertThat(ip).isEqualTo("1.2.3.4");
        }

        @Test
        void ip_없이_제출된_신고는_null을_반환한다() {
            final Report saved = reportRepository.save(
                    Report.create(ReportCreation.general(ReportCategory.SUGGESTION, "내용", null, null),
                            Clock.systemUTC())
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

    @Nested
    @DisplayName("unblockReporterIp")
    class UnblockReporterIp {

        @Autowired
        private IpBlockStore ipBlockStore;

        @Test
        void 차단된_신고자_IP를_해제한다() {
            final Ip ip = new Ip("9.8.7.6");
            ipBlockStore.blockImmediately(ip);
            final Report saved = reportRepository.save(
                    Report.create(ReportCreation.bug(MiniGameType.CARD_GAME, "ABC12", "내용", null, "9.8.7.6"),
                            Clock.systemUTC())
            );

            reportAdminService.unblockReporterIp(saved.getId());

            assertThat(ipBlockStore.isBlocked(ip)).isFalse();
        }

        @Test
        void ip_없이_제출된_신고는_예외_없이_무시한다() {
            final Report saved = reportRepository.save(
                    Report.create(ReportCreation.general(ReportCategory.SUGGESTION, "내용", null, null),
                            Clock.systemUTC())
            );

            assertThatCode(() -> reportAdminService.unblockReporterIp(saved.getId()))
                    .doesNotThrowAnyException();
        }

        @Test
        void 존재하지_않는_신고_id면_예외를_던진다() {
            assertCoffeeShoutException(
                    () -> reportAdminService.unblockReporterIp(9999L),
                    GlobalErrorCode.NOT_EXIST
            );
        }
    }
}
