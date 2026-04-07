package coffeeshout.global.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("MaliciousPathMatcher")
class MaliciousPathMatcherTest {

    private final MaliciousPathMatcher matcher = new MaliciousPathMatcher();

    @Nested
    @DisplayName("악성 경로 감지")
    class 악성_경로_감지 {

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {
                "/.env",
                "/.env.local",
                "/.env.production",
                "/.git",
                "/.git/config",
                "/.git/HEAD",
                "/.aws",
                "/.aws/credentials",
                "/.ssh",
                "/.ssh/id_rsa",
                "/wp-admin",
                "/wp-admin/admin.php",
                "/wp-login.php",
                "/wp-content/themes/shell.php",
                "/phpmyadmin",
                "/phpmyadmin/index.php",
                "/xmlrpc.php",
                "/cgi-bin",
                "/cgi-bin/test.cgi",
                "/admin.php",
                "/config.php",
                "/setup.php",
                "/install.php"
        })
        void 악성_경로_패턴은_true를_반환한다(final String path) {
            assertThat(matcher.isMalicious(path)).isTrue();
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {
                "/.ENV",
                "/.Git",
                "/WP-ADMIN",
                "/XMLRPC.PHP"
        })
        void 대소문자_구분_없이_악성_경로를_감지한다(final String path) {
            assertThat(matcher.isMalicious(path)).isTrue();
        }
    }

    @Nested
    @DisplayName("정상 경로 허용")
    class 정상_경로_허용 {

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {
                "/",
                "/reports",
                "/admin",
                "/admin/login",
                "/admin/reports",
                "/ws",
                "/api/rooms",
                "/health"
        })
        void 정상_경로는_false를_반환한다(final String path) {
            assertThat(matcher.isMalicious(path)).isFalse();
        }
    }
}
