package coffeeshout.admin.quality.ui;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import coffeeshout.admin.support.AdminApiE2eTest;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.report.domain.ReportCategory;
import coffeeshout.report.infra.persistence.Report;
import coffeeshout.report.infra.persistence.ReportRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("운영 품질 API")
class AdminQualityControllerE2eTest extends AdminApiE2eTest {

    @Autowired
    private ReportRepository reportRepository;

    private void givenPendingReport(ReportCategory category, MiniGameType gameType) {
        final Report report = gameType == null
                ? Report.createGeneralReport(category, "내용", Instant.now())
                : Report.createBugReport(gameType, "ABCD", "내용", Instant.now());
        reportRepository.save(report);
    }

    private void givenResolvedReport(long minutesToResolve) {
        final Report report = Report.createGeneralReport(
                ReportCategory.BUG, "처리된 신고", Instant.now().minus(minutesToResolve, ChronoUnit.MINUTES));
        report.resolve();
        reportRepository.save(report);
    }

    @Nested
    class 신고_적체 {

        @Test
        void 미처리_건수와_가장_오래_기다린_건의_나이를_돌려준다() throws Exception {
            givenPendingReport(ReportCategory.BUG, null);
            givenResolvedReport(30);

            mockMvc.perform(get("/admin/api/quality/report-backlog").with(admin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pendingCount").value(1))
                    .andExpect(jsonPath("$.oldestPendingMinutes").exists())
                    // 처리 시간 백분위는 걷어냈다. 신고가 하루에 몇 건뿐이라 한 건이
                    // 들어오고 나갈 때마다 크게 흔들렸다.
                    .andExpect(jsonPath("$.p50Minutes").doesNotExist())
                    .andExpect(jsonPath("$.p95Minutes").doesNotExist());
        }
    }

    @Nested
    class 신고_지표 {

        @Test
        void 카테고리는_신고가_없어도_모두_돌려준다() throws Exception {
            // 화면이 빠진 카테고리를 채우게 두면 카테고리가 하나 늘었을 때 칸이 사라진다.
            givenPendingReport(ReportCategory.BUG, null);

            mockMvc.perform(get("/admin/api/quality/report-stats")
                            .param("days", "30")
                            .with(admin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(1))
                    .andExpect(jsonPath("$.categories.length()").value(ReportCategory.values().length))
                    .andExpect(jsonPath("$.daily.length()").value(30));
        }

        @Test
        void 게임에_붙은_신고는_게임_이름을_함께_준다() throws Exception {
            // 한글 이름을 서버가 붙인다. 화면이 대응표를 따로 들면 게임이 늘었을 때
            // 빠진 항목이 영문 그대로 노출된다.
            givenPendingReport(ReportCategory.BUG, MiniGameType.CARD_GAME);

            mockMvc.perform(get("/admin/api/quality/report-stats")
                            .param("days", "30")
                            .with(admin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.games[0].gameType").value("CARD_GAME"))
                    .andExpect(jsonPath("$.games[0].label").value(MiniGameType.CARD_GAME.label));
        }

        @Test
        void 기간_상한을_넘기면_400_이다() throws Exception {
            mockMvc.perform(get("/admin/api/quality/report-stats")
                            .param("days", "91")
                            .with(admin()))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class 검열_지표 {

        @Test
        void 판정은_하나도_없어도_모두_돌려준다() throws Exception {
            mockMvc.perform(get("/admin/api/quality/nickname-audit-stats")
                            .param("days", "30")
                            .with(admin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(0))
                    .andExpect(jsonPath("$.statuses.length()").value(7))
                    .andExpect(jsonPath("$.daily.length()").value(30));
        }

        @Test
        void 판정_정확도를_돌려준다() throws Exception {
            mockMvc.perform(get("/admin/api/quality/nickname-audit")
                            .param("days", "30")
                            .with(admin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").exists())
                    .andExpect(jsonPath("$.overrideRate").exists())
                    .andExpect(jsonPath("$.sampleReviewed").value(0));
        }
    }

    @Test
    void 토큰이_없으면_401_이다() throws Exception {
        mockMvc.perform(get("/admin/api/quality/report-backlog")).andExpect(status().isUnauthorized());
    }
}
