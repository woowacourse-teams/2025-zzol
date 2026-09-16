package coffeeshout.admin.account.domain;

import static coffeeshout.support.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("AdminEmail")
class AdminEmailTest {

    @Nested
    class 정규화 {

        @ParameterizedTest
        @CsvSource({
            "MJ@ZZOL.SITE, mj@zzol.site",
            "'  mj@zzol.site  ', mj@zzol.site",
            "Mj@Zzol.Site, mj@zzol.site",
            "mj@zzol.site, mj@zzol.site"
        })
        void 공백을_제거하고_소문자로_낮춘다(String raw, String expected) {
            assertThat(AdminEmail.of(raw).value()).isEqualTo(expected);
        }

        @Test
        void 대소문자만_다른_두_값은_같다() {
            // 환경변수와 DB가 서로 다른 규칙으로 다루면 등록한 계정으로 로그인이 안 된다.
            assertThat(AdminEmail.of("MJ@Zzol.Site")).isEqualTo(AdminEmail.of("mj@zzol.site"));
        }

        @Test
        void toString은_값을_그대로_돌려준다() {
            assertThat(AdminEmail.of("mj@zzol.site")).hasToString("mj@zzol.site");
        }
    }

    @Nested
    class of {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        void 비어있으면_예외를_던진다(String raw) {
            assertCoffeeShoutException(() -> AdminEmail.of(raw), AdminAccountErrorCode.INVALID_ADMIN_EMAIL);
        }

        @ParameterizedTest
        @ValueSource(strings = {"nobody", "@zzol.site", "mj@", "mj@a@b.site"})
        void 형식이_아니면_예외를_던진다(String raw) {
            assertCoffeeShoutException(() -> AdminEmail.of(raw), AdminAccountErrorCode.INVALID_ADMIN_EMAIL);
        }

        @Test
        void 길이가_255자를_넘으면_예외를_던진다() {
            final String tooLong = "a".repeat(250) + "@zzol.site";

            assertCoffeeShoutException(() -> AdminEmail.of(tooLong), AdminAccountErrorCode.INVALID_ADMIN_EMAIL);
        }

        @Test
        void 경계값_255자는_허용한다() {
            final String exact = "a".repeat(255 - "@zzol.site".length()) + "@zzol.site";

            assertThat(AdminEmail.of(exact).value()).hasSize(255);
        }
    }

    @Nested
    class parse {

        @Test
        void 유효하면_값을_담아_돌려준다() {
            assertThat(AdminEmail.parse("  MJ@Zzol.Site ")).contains(AdminEmail.of("mj@zzol.site"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "nobody", "mj@"})
        void 유효하지_않으면_빈_값을_돌려준다(String raw) {
            // 로그인 판정에서는 형식이 틀린 것과 목록에 없는 것을 구분해 주면 안 된다.
            assertThat(AdminEmail.parse(raw)).isEmpty();
        }
    }
}
