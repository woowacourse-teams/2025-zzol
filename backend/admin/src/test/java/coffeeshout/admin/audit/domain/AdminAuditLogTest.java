package coffeeshout.admin.audit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("AdminAuditLog")
class AdminAuditLogTest {

    private static final Instant NOW = Instant.parse("2026-09-06T00:00:00Z");

    private static AdminAuditLog of(String actor, String action) {
        return AdminAuditLog.of(actor, action, "accounts", "1", null, AdminAuditResult.SUCCESS, NOW);
    }

    @Nested
    class 생성 {

        @Test
        void 주어진_값을_그대로_보관한다() {
            final AdminAuditLog log = AdminAuditLog.of(
                    "mj@zzol.site",
                    "DELETE /admin/api/accounts/{id}",
                    "accounts",
                    "7",
                    "사유",
                    AdminAuditResult.SUCCESS,
                    NOW);

            assertThat(log.getActorEmail()).isEqualTo("mj@zzol.site");
            assertThat(log.getAction()).isEqualTo("DELETE /admin/api/accounts/{id}");
            assertThat(log.getTargetType()).isEqualTo("accounts");
            assertThat(log.getTargetId()).isEqualTo("7");
            assertThat(log.getDetail()).isEqualTo("사유");
            assertThat(log.getResult()).isEqualTo(AdminAuditResult.SUCCESS);
            assertThat(log.getCreatedAt()).isEqualTo(NOW);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void 실행자가_없으면_anonymous로_남긴다(String actor) {
            // 로그인 시도는 인증 전이라 주체가 없다. 그래도 기록은 남아야 한다.
            assertThat(of(actor, "POST /admin/api/auth/login").getActorEmail()).isEqualTo("anonymous");
        }

        @Test
        void 실패도_기록한다() {
            final AdminAuditLog log = AdminAuditLog.of(
                    "mj@zzol.site",
                    "POST /admin/api/accounts",
                    "accounts",
                    null,
                    "BusinessException: 이미 등록된 관리자입니다.",
                    AdminAuditResult.FAILURE,
                    NOW);

            assertThat(log.getResult()).isEqualTo(AdminAuditResult.FAILURE);
            assertThat(log.getDetail()).contains("이미 등록된 관리자");
        }
    }

    @Nested
    class 길이_초과 {

        @Test
        void action이_100자를_넘으면_잘라서라도_남긴다() {
            // 길이 초과로 감사 기록이 통째로 유실되면 안 된다.
            final AdminAuditLog log = of("mj@zzol.site", "P".repeat(150));

            assertThat(log.getAction()).hasSize(100);
        }

        @Test
        void 실행자_이메일이_255자를_넘으면_자른다() {
            final AdminAuditLog log = of("a".repeat(300), "POST /x");

            assertThat(log.getActorEmail()).hasSize(255);
        }

        @Test
        void detail은_TEXT_컬럼이라_자르지_않는다() {
            final String longDetail = "x".repeat(5000);
            final AdminAuditLog log =
                    AdminAuditLog.of("mj@zzol.site", "POST /x", null, null, longDetail, AdminAuditResult.FAILURE, NOW);

            assertThat(log.getDetail()).hasSize(5000);
        }

        @Test
        void 경계값은_자르지_않는다() {
            final AdminAuditLog log = of("mj@zzol.site", "P".repeat(100));

            assertThat(log.getAction()).hasSize(100);
        }
    }
}
