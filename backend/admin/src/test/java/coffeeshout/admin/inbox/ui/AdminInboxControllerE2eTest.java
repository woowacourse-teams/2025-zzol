package coffeeshout.admin.inbox.ui;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import coffeeshout.admin.support.AdminApiE2eTest;
import coffeeshout.global.outbox.OutboxEvent;
import coffeeshout.global.outbox.OutboxEventRepository;
import coffeeshout.profanity.application.port.NicknameAuditRepository;
import coffeeshout.profanity.domain.audit.AiConfidence;
import coffeeshout.profanity.domain.audit.NicknameAudit;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import coffeeshout.report.domain.ReportCategory;
import coffeeshout.report.infra.persistence.Report;
import coffeeshout.report.infra.persistence.ReportRepository;
import java.time.Instant;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 작업함은 세 곳에서 모은다. 컨트롤러 단위 테스트로는 그 셋이 실제로 한 목록에 섞이는지
 * 확인할 수 없다.
 */
@DisplayName("통합 작업함 API")
class AdminInboxControllerE2eTest extends AdminApiE2eTest {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private NicknameAuditRepository nicknameAuditRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    void 신고와_닉네임과_격리를_한_목록으로_준다() throws Exception {
        reportRepository.save(Report.createGeneralReport(ReportCategory.BUG, "카드가 안 보여요", Instant.now()));

        final NicknameAudit audit = new NicknameAudit("씨b알");
        audit.complete(NicknameAuditStatus.FLAGGED, AiConfidence.of(0.97), "비속어 우회");
        nicknameAuditRepository.save(audit);

        final OutboxEvent event = OutboxEvent.create("settlement:result", "{}");
        event.markDeadLetter();
        outboxEventRepository.save(event);

        mockMvc.perform(get("/admin/api/inbox").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(
                        jsonPath("$[*].kind").value(Matchers.containsInAnyOrder("REPORT", "NICKNAME", "DEAD_LETTER")));
    }

    @Test
    void 처리할_것이_없으면_빈_목록이다() throws Exception {
        mockMvc.perform(get("/admin/api/inbox").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void 이미_처리한_신고는_담지_않는다() throws Exception {
        final Report resolved = Report.createGeneralReport(ReportCategory.BUG, "이미 처리함", Instant.now());
        resolved.resolve();
        reportRepository.save(resolved);

        mockMvc.perform(get("/admin/api/inbox").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void 토큰이_없으면_401_이다() throws Exception {
        mockMvc.perform(get("/admin/api/inbox")).andExpect(status().isUnauthorized());
    }
}
