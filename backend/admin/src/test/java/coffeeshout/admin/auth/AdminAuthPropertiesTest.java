package coffeeshout.admin.auth;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.admin.account.domain.AdminEmail;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AdminAuthProperties")
class AdminAuthPropertiesTest {

    private static final String SECRET = "x".repeat(32);
    private static final AdminEmail MJ = AdminEmail.of("mj@zzol.site");
    private static final AdminEmail ROOT = AdminEmail.of("root@zzol.site");

    private static AdminAuthProperties withEmails(List<String> emails) {
        return new AdminAuthProperties(emails, "client-id", SECRET, 3600);
    }

    @Nested
    class bootstrapEmails {

        @Test
        void 대소문자와_공백을_정규화해_보관한다() {
            final AdminAuthProperties properties = withEmails(List.of("  MJ@Zzol.Site ", "root@ZZOL.site"));

            assertThat(properties.bootstrapEmails()).containsExactly(MJ, ROOT);
        }

        @Test
        void 형식이_어긋난_항목은_조용히_걸러낸다() {
            // 환경변수 오타 하나로 앱이 아예 안 뜨면 배포가 막힌다.
            final AdminAuthProperties properties = withEmails(Arrays.asList("mj@zzol.site", "", "   ", "오타", null));

            assertThat(properties.bootstrapEmails()).containsExactly(MJ);
        }

        @Test
        void 중복은_한_번만_남기고_순서를_유지한다() {
            final AdminAuthProperties properties =
                    withEmails(List.of("root@zzol.site", "MJ@zzol.site", "mj@zzol.site"));

            assertThat(properties.bootstrapEmails()).containsExactly(ROOT, MJ);
        }

        @Test
        void 환경변수가_비어_있으면_빈_목록이다() {
            // ADMIN_EMAILS 미설정 시 Spring이 null을 바인딩한다. 여기서 터지면 앱이 안 뜬다.
            assertThat(withEmails(null).bootstrapEmails()).isEmpty();
        }
    }

    @Nested
    class isBootstrap {

        private final AdminAuthProperties properties = withEmails(List.of("mj@zzol.site"));

        @Test
        void 목록에_있으면_참이다() {
            assertThat(properties.isBootstrap(MJ)).isTrue();
        }

        @Test
        void 대소문자와_공백이_달라도_참이다() {
            assertThat(properties.isBootstrap(AdminEmail.of("  MJ@Zzol.Site  ")))
                    .isTrue();
        }

        @Test
        void 목록에_없으면_거짓이다() {
            assertThat(properties.isBootstrap(AdminEmail.of("stranger@zzol.site")))
                    .isFalse();
        }

        @Test
        void null은_거짓이다() {
            assertThat(properties.isBootstrap(null)).isFalse();
        }

        @Test
        void 부트스트랩이_비어_있으면_어떤_이메일도_거짓이다() {
            assertThat(withEmails(List.of()).isBootstrap(MJ)).isFalse();
        }
    }
}
