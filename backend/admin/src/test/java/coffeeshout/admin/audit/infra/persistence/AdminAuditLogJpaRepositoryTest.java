package coffeeshout.admin.audit.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.AdminModuleServiceTest;
import coffeeshout.admin.audit.domain.AdminAuditLog;
import coffeeshout.admin.audit.domain.AdminAuditLogRepository;
import coffeeshout.admin.audit.domain.AdminAuditResult;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

@DisplayName("AdminAuditLogJpaRepository")
class AdminAuditLogJpaRepositoryTest extends AdminModuleServiceTest {

    private static final PageRequest FIRST_PAGE = PageRequest.of(0, 20);

    @Autowired
    private AdminAuditLogRepository adminAuditLogRepository;

    private void given(String actorEmail) {
        adminAuditLogRepository.save(AdminAuditLog.of(
                actorEmail,
                "POST /admin/api/reports/{id}/resolve",
                "reports",
                "1",
                null,
                AdminAuditResult.SUCCESS,
                Instant.now()));
    }

    @Nested
    class search {

        @Test
        void 담당자를_부분_일치로_찾는다() {
            given("mj@zzol.site");
            given("ops@zzol.site");

            assertThat(adminAuditLogRepository
                            .search("mj", null, null, FIRST_PAGE)
                            .getContent())
                    .extracting(AdminAuditLog::getActorEmail)
                    .containsExactly("mj@zzol.site");
        }

        @Test
        void 검색어의_와일드카드는_글자_그대로_찾는다() {
            // LIKE 의 _ 는 아무 글자 하나와 맞는다. 이스케이프하지 않으면 "mj_admin" 을
            // 찾을 때 "mjXadmin" 까지 걸린다. 관리자 이메일에는 밑줄이 흔하다.
            given("mj_admin@zzol.site");
            given("mjXadmin@zzol.site");

            assertThat(adminAuditLogRepository
                            .search("mj_admin", null, null, FIRST_PAGE)
                            .getContent())
                    .extracting(AdminAuditLog::getActorEmail)
                    .containsExactly("mj_admin@zzol.site");
        }

        @Test
        void 퍼센트를_쳐도_전체가_걸리지_않는다() {
            given("mj@zzol.site");
            given("ops@zzol.site");

            assertThat(adminAuditLogRepository
                            .search("%", null, null, FIRST_PAGE)
                            .getContent())
                    .isEmpty();
        }

        @Test
        void 담당자를_비우면_전체를_돌려준다() {
            given("mj@zzol.site");
            given("ops@zzol.site");

            assertThat(adminAuditLogRepository
                            .search(null, null, null, FIRST_PAGE)
                            .getTotalElements())
                    .isEqualTo(2);
        }
    }
}
