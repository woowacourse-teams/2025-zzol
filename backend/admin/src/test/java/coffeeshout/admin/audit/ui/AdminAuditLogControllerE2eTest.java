package coffeeshout.admin.audit.ui;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import coffeeshout.admin.audit.domain.AdminAuditLog;
import coffeeshout.admin.audit.domain.AdminAuditLogRepository;
import coffeeshout.admin.audit.domain.AdminAuditResult;
import coffeeshout.admin.support.AdminApiE2eTest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("조치 이력 API")
class AdminAuditLogControllerE2eTest extends AdminApiE2eTest {

    @Autowired
    private AdminAuditLogRepository adminAuditLogRepository;

    private void given(String actorEmail, AdminAuditResult result, Instant at) {
        adminAuditLogRepository.save(
                AdminAuditLog.of(actorEmail, "POST /admin/api/reports/{id}/resolve", "reports", "1", null, result, at));
    }

    @Nested
    class list {

        @Test
        void 최근_조치부터_돌려준다() throws Exception {
            final Instant now = Instant.now();
            given("mj@zzol.site", AdminAuditResult.SUCCESS, now.minus(1, ChronoUnit.HOURS));
            given("ops@zzol.site", AdminAuditResult.SUCCESS, now);

            mockMvc.perform(get("/admin/api/audit-logs").with(admin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.content[0].actorEmail").value("ops@zzol.site"));
        }

        @Test
        void 담당자로_거른다() throws Exception {
            given("mj@zzol.site", AdminAuditResult.SUCCESS, Instant.now());
            given("ops@zzol.site", AdminAuditResult.SUCCESS, Instant.now());

            mockMvc.perform(get("/admin/api/audit-logs")
                            .param("actorEmail", "mj")
                            .with(admin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        void 실패한_조치만_거른다() throws Exception {
            given("mj@zzol.site", AdminAuditResult.SUCCESS, Instant.now());
            given("mj@zzol.site", AdminAuditResult.FAILURE, Instant.now());

            mockMvc.perform(get("/admin/api/audit-logs")
                            .param("result", "FAILURE")
                            .with(admin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].result").value("FAILURE"));
        }

        @Test
        void 기간_밖의_조치는_빠진다() throws Exception {
            given("mj@zzol.site", AdminAuditResult.SUCCESS, Instant.now().minus(10, ChronoUnit.DAYS));

            mockMvc.perform(get("/admin/api/audit-logs").param("days", "1").with(admin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        void 검색창을_비우면_조건이_걸리지_않는다() throws Exception {
            // 화면이 빈 칸을 actorEmail= 로 보낸다. 그대로 LIKE 에 넣으면 조건이 걸린 것도
            // 아닌 것도 아닌 상태가 된다.
            given("mj@zzol.site", AdminAuditResult.SUCCESS, Instant.now());

            mockMvc.perform(get("/admin/api/audit-logs").param("actorEmail", "").with(admin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        void 한_번에_너무_많이_달라고_하면_400_이다() throws Exception {
            mockMvc.perform(get("/admin/api/audit-logs").param("size", "1000").with(admin()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void 토큰이_없으면_401_이다() throws Exception {
            mockMvc.perform(get("/admin/api/audit-logs")).andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class stats {

        @Test
        void 일자별_성공과_실패를_돌려준다() throws Exception {
            given("mj@zzol.site", AdminAuditResult.SUCCESS, Instant.now());
            given("mj@zzol.site", AdminAuditResult.FAILURE, Instant.now());

            mockMvc.perform(get("/admin/api/audit-logs/stats")
                            .param("days", "7")
                            .with(admin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(2))
                    .andExpect(jsonPath("$.failed").value(1))
                    .andExpect(jsonPath("$.daily.length()").value(7))
                    .andExpect(jsonPath("$.actors[0].actorEmail").value("mj@zzol.site"));
        }

        @Test
        void 기간_상한을_넘기면_400_이다() throws Exception {
            mockMvc.perform(get("/admin/api/audit-logs/stats")
                            .param("days", "365")
                            .with(admin()))
                    .andExpect(status().isBadRequest());
        }
    }
}
