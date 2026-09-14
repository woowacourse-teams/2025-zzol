package coffeeshout.admin.ops.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import coffeeshout.admin.audit.domain.AdminAuditLog;
import coffeeshout.admin.audit.domain.AdminAuditLogRepository;
import coffeeshout.admin.support.AdminApiE2eTest;
import coffeeshout.global.outbox.OutboxEvent;
import coffeeshout.global.outbox.OutboxEventRepository;
import coffeeshout.global.outbox.OutboxStatus;
import coffeeshout.settlement.infra.persistence.SettlementDeadLetterEntity;
import coffeeshout.settlement.infra.persistence.SettlementDeadLetterJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

@DisplayName("시스템 운영 API")
class AdminOpsControllerE2eTest extends AdminApiE2eTest {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private SettlementDeadLetterJpaRepository settlementDeadLetterRepository;

    @Autowired
    private AdminAuditLogRepository adminAuditLogRepository;

    private OutboxEvent givenDeadLetter() {
        final OutboxEvent event = OutboxEvent.create("settlement:result", "{\"roomId\":1}");
        event.markDeadLetter();
        return outboxEventRepository.save(event);
    }

    private OutboxEvent givenPending() {
        return outboxEventRepository.save(OutboxEvent.create("settlement:result", "{\"roomId\":2}"));
    }

    @Nested
    class 격리_목록 {

        @Test
        void 격리된_것만_돌려준다() throws Exception {
            givenDeadLetter();
            givenPending();

            mockMvc.perform(get("/admin/api/ops/dead-letters")
                            .param("source", "OUTBOX")
                            .with(admin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].source").value("OUTBOX"))
                    .andExpect(jsonPath("$.content[0].payload").value("{\"roomId\":1}"));
        }

        @Test
        void 정산_큐도_같은_모양으로_돌려준다() throws Exception {
            settlementDeadLetterRepository.save(new SettlementDeadLetterEntity("record-1", "JSON 파싱 실패", "{}"));

            mockMvc.perform(get("/admin/api/ops/dead-letters")
                            .param("source", "SETTLEMENT")
                            .with(admin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].reference").value("record-1"))
                    .andExpect(jsonPath("$.content[0].reason").value("JSON 파싱 실패"));
        }
    }

    @Nested
    class 다시_넣기 {

        @Test
        void 상태를_PENDING_으로_되돌린다() throws Exception {
            final OutboxEvent event = givenDeadLetter();

            mockMvc.perform(post("/admin/api/ops/dead-letters/outbox/{id}/requeue", event.getId())
                            .with(admin()))
                    .andExpect(status().isNoContent());

            assertThat(outboxEventRepository.findById(event.getId()))
                    .get()
                    .extracting(OutboxEvent::getStatus)
                    .isEqualTo(OutboxStatus.PENDING);
        }

        @Test
        void 격리_상태가_아니면_409_다() throws Exception {
            final OutboxEvent pending = givenPending();

            mockMvc.perform(post("/admin/api/ops/dead-letters/outbox/{id}/requeue", pending.getId())
                            .with(admin()))
                    .andExpect(status().isConflict());
        }

        @Test
        void 없는_메시지는_404_다() throws Exception {
            mockMvc.perform(post("/admin/api/ops/dead-letters/outbox/{id}/requeue", 999_999L)
                            .with(admin()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class 폐기 {

        @Test
        void 행을_지우고_감사_로그를_남긴다() throws Exception {
            final OutboxEvent event = givenDeadLetter();

            mockMvc.perform(delete("/admin/api/ops/dead-letters/{source}/{id}", "OUTBOX", event.getId())
                            .with(admin()))
                    .andExpect(status().isNoContent());

            assertThat(outboxEventRepository.findById(event.getId())).isEmpty();
            // 되돌릴 수 없는 조치다. 누가 언제 지웠는지가 남지 않으면 나중에 되짚을 방법이 없다.
            assertThat(adminAuditLogRepository
                            .findAllByOrderByCreatedAtDesc(PageRequest.of(0, 10))
                            .getContent())
                    .extracting(AdminAuditLog::getAction, AdminAuditLog::getActorEmail)
                    .contains(org.assertj.core.api.Assertions.tuple(
                            "DELETE /admin/api/ops/dead-letters/{source}/{id}", ACTOR));
        }

        @Test
        void 격리_상태가_아닌_것은_지우지_않는다() throws Exception {
            // outbox 에는 발행을 기다리는 행이 함께 산다. id 만 보고 지우면 아직 나가지 않은
            // 도메인 이벤트가 사라진다.
            final OutboxEvent pending = givenPending();

            mockMvc.perform(delete("/admin/api/ops/dead-letters/{source}/{id}", "OUTBOX", pending.getId())
                            .with(admin()))
                    .andExpect(status().isConflict());

            assertThat(outboxEventRepository.findById(pending.getId())).isPresent();
        }

        @Test
        void 없는_정산_메시지는_404_다() throws Exception {
            mockMvc.perform(delete("/admin/api/ops/dead-letters/{source}/{id}", "SETTLEMENT", 999_999L)
                            .with(admin()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class 스키마와_배포 {

        @Test
        void 마이그레이션_이력을_돌려준다() throws Exception {
            // 테스트 DB 는 ddl-auto 로 만들어 flyway 이력 테이블이 없다. 그 사실을 그대로
            // 알려 주는 것이 이 API 의 일이다.
            mockMvc.perform(get("/admin/api/ops/migrations").with(admin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.managed").value(false));
        }

        @Test
        void 배포_정보를_돌려준다() throws Exception {
            mockMvc.perform(get("/admin/api/ops/deployment").with(admin())).andExpect(status().isOk());
        }
    }
}
