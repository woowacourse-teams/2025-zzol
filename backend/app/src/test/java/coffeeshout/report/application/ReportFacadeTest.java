package coffeeshout.report.application;

import static coffeeshout.support.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.support.app.ServiceTest;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.report.domain.ReportCategory;
import coffeeshout.report.domain.ReportErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ReportFacadeTest extends ServiceTest {

    @Autowired
    private ReportFacade reportFacade;

    private static final String IP = "1.2.3.4";
    private static final String ANOTHER_IP = "5.6.7.8";

    @Nested
    @DisplayName("submit - IP 검증")
    class SubmitIpValidation {

        @Test
        void IP가_null이면_예외를_던진다() {
            assertCoffeeShoutException(
                    () -> reportFacade.submit(null, ReportCategory.SUGGESTION, null, null, "내용"),
                    ReportErrorCode.INVALID_CLIENT_IP
            );
        }

        @Test
        void IP가_빈_문자열이면_예외를_던진다() {
            assertCoffeeShoutException(
                    () -> reportFacade.submit("  ", ReportCategory.SUGGESTION, null, null, "내용"),
                    ReportErrorCode.INVALID_CLIENT_IP
            );
        }
    }

    @Nested
    @DisplayName("submit - rate limit")
    class SubmitRateLimit {

        @Test
        void 동일_IP에서_임계값_이내_요청은_모두_성공한다() {
            for (int i = 0; i < 5; i++) {
                final int seq = i;
                assertThat(reportFacade.submit(IP, ReportCategory.SUGGESTION, null, null, "내용" + seq))
                        .isPositive();
            }
        }

        @Test
        void 동일_IP에서_임계값_초과_요청은_예외를_던진다() {
            for (int i = 0; i < 5; i++) {
                reportFacade.submit(IP, ReportCategory.SUGGESTION, null, null, "내용" + i);
            }

            assertCoffeeShoutException(
                    () -> reportFacade.submit(IP, ReportCategory.SUGGESTION, null, null, "초과 요청"),
                    ReportErrorCode.REPORT_RATE_LIMITED
            );
        }

        @Test
        void 다른_IP는_rate_limit이_독립적으로_적용된다() {
            for (int i = 0; i < 5; i++) {
                reportFacade.submit(IP, ReportCategory.SUGGESTION, null, null, "내용" + i);
            }

            assertThat(reportFacade.submit(ANOTHER_IP, ReportCategory.SUGGESTION, null, null, "다른 IP 요청"))
                    .isPositive();
        }
    }

    @Nested
    @DisplayName("submit - 정상 위임")
    class SubmitDelegate {

        @Test
        void 유효한_요청은_저장된_id를_반환한다() {
            final long id = reportFacade.submit(
                    IP, ReportCategory.BUG, MiniGameType.CARD_GAME, "ABC12", "카드게임이 멈춰요."
            );

            assertThat(id).isPositive();
        }
    }
}
