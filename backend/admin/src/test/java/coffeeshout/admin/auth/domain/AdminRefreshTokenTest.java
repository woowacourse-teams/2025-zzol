package coffeeshout.admin.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("AdminRefreshToken")
class AdminRefreshTokenTest {

    private static final String ID_16 = "abcdefghijklmnop";

    @Nested
    class parse {

        @Test
        void 새로_만든_토큰의_값을_그대로_되읽는다() {
            final AdminRefreshToken token = AdminRefreshToken.newFamily();

            assertThat(AdminRefreshToken.parse(token.value())).contains(token);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(
                strings = {
                    "구분자가없는값abcdefghijklmnop",
                    ID_16 + "." + ID_16 + "." + ID_16,
                    "." + ID_16,
                    ID_16 + ".",
                    // 15자는 너무 짧다
                    "abcdefghijklmno." + ID_16,
                    // 65자는 너무 길다. 쿠키 값이 그대로 Redis 키가 되므로 상한을 둔다
                    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa." + ID_16,
                    // base64url 밖의 문자
                    "abcdefghijklmno:" + "." + ID_16,
                    "abcdefghijklmn*o." + ID_16
                })
        void 형식이_어긋나면_빈_값이다(String raw) {
            assertThat(AdminRefreshToken.parse(raw)).isEmpty();
        }

        @Test
        void 경계_길이_16자와_64자는_받는다() {
            final String id64 = "a".repeat(64);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(AdminRefreshToken.parse(ID_16 + "." + ID_16)).isPresent();
                softly.assertThat(AdminRefreshToken.parse(id64 + "." + id64)).isPresent();
            });
        }
    }

    @Nested
    class rotate {

        @Test
        void family는_유지하고_tokenId만_바꾼다() {
            final AdminRefreshToken token = AdminRefreshToken.newFamily();

            final AdminRefreshToken next = token.rotate();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(next.familyId()).isEqualTo(token.familyId());
                softly.assertThat(next.tokenId()).isNotEqualTo(token.tokenId());
            });
        }
    }

    @Test
    void 새_family는_매번_다르다() {
        assertThat(AdminRefreshToken.newFamily().familyId())
                .isNotEqualTo(AdminRefreshToken.newFamily().familyId());
    }
}
