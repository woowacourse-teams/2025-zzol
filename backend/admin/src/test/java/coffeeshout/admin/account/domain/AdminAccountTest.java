package coffeeshout.admin.account.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AdminAccount")
class AdminAccountTest {

    private static final Instant NOW = Instant.parse("2026-09-06T00:00:00Z");
    private static final AdminEmail MJ = AdminEmail.of("mj@zzol.site");
    private static final AdminEmail ROOT = AdminEmail.of("root@zzol.site");

    @Nested
    class create {

        @Test
        void 이메일과_추가자와_생성_시각을_보관한다() {
            final AdminAccount account = AdminAccount.create(MJ, ROOT, NOW);

            assertThat(account.getEmail()).isEqualTo(MJ);
            assertThat(account.getCreatedByEmail()).isEqualTo(ROOT);
            assertThat(account.getCreatedAt()).isEqualTo(NOW);
        }

        @Test
        void 시스템이_추가한_경우_추가자가_없다() {
            final AdminAccount account = AdminAccount.create(MJ, null, NOW);

            assertThat(account.getCreatedByEmail()).isNull();
        }

        @Test
        void 저장한_값을_다시_읽어도_정규화된_상태를_유지한다() {
            // 컬럼은 문자열이지만 밖으로는 AdminEmail 만 나가야 한다.
            final AdminAccount account = AdminAccount.create(AdminEmail.of("  MJ@Zzol.Site "), null, NOW);

            assertThat(account.getEmail().value()).isEqualTo("mj@zzol.site");
        }
    }
}
