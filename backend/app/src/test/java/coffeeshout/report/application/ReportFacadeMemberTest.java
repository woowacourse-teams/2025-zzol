package coffeeshout.report.application;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.support.ServiceTest;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.report.domain.ReportCategory;
import coffeeshout.report.infra.persistence.Report;
import coffeeshout.report.infra.persistence.ReportRepository;
import coffeeshout.user.domain.AuthenticatedUser;
import java.util.Optional;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ReportFacadeMemberTest extends ServiceTest {

    @Autowired
    ReportFacade reportFacade;

    @Autowired
    ReportRepository reportRepository;

    private static final String 회원_IP = "127.0.0.1";
    private static final String 익명_IP = "127.0.0.2";
    private static final AuthenticatedUser 회원_인증 = new AuthenticatedUser(1L, "AB3CD");

    @Nested
    class 회원이_신고할_때 {

        @Test
        void userId와_userCode가_저장된다() {
            final long id = reportFacade.submit(
                    회원_IP,
                    ReportCategory.BUG,
                    MiniGameType.CARD_GAME,
                    "ABC12",
                    "버그 내용",
                    Optional.of(회원_인증)
            );

            final Report saved = reportRepository.findById(id).orElseThrow();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(saved.getAuthor().getUserId()).isEqualTo(1L);
                softly.assertThat(saved.getAuthor().getUserCode()).isEqualTo("AB3CD");
            });
        }
    }

    @Nested
    class 익명이_신고할_때 {

        @Test
        void userId와_userCode가_null이다() {
            final long id = reportFacade.submit(
                    익명_IP,
                    ReportCategory.SUGGESTION,
                    null,
                    null,
                    "건의 내용",
                    Optional.empty()
            );

            final Report saved = reportRepository.findById(id).orElseThrow();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(saved.getAuthor()).isNull();
            });
        }
    }
}
