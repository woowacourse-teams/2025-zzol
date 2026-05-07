package coffeeshout.global.zzolbot.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PiiMaskingSessionTest {

    @Nested
    class mask_메서드 {

        @Test
        void null_입력_시_null을_반환한다() {
            final PiiMaskingSession session = PiiMaskingSession.forSeed(42L);

            assertThat(session.mask(null)).isNull();
        }

        @Test
        void 마스킹_대상이_없으면_원문을_반환한다() {
            final PiiMaskingSession session = PiiMaskingSession.forSeed(42L);
            final String text = "joinCode=A4BX, 상태 정상";

            assertThat(session.mask(text)).isEqualTo(text);
        }

        @Test
        void 이메일_주소를_EMAIL_xxxx_형태_토큰으로_마스킹한다() {
            final PiiMaskingSession session = PiiMaskingSession.forSeed(42L);

            final String masked = session.mask("user@example.com 접속");

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(masked).doesNotContain("user@example.com");
                softly.assertThat(masked).matches(".*\\[EMAIL_[0-9a-f]{4}\\].*");
            });
        }

        @Test
        void IP_주소를_IP_xxxx_형태_토큰으로_마스킹한다() {
            final PiiMaskingSession session = PiiMaskingSession.forSeed(42L);

            final String masked = session.mask("192.168.1.100 접속");

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(masked).doesNotContain("192.168.1.100");
                softly.assertThat(masked).matches(".*\\[IP_[0-9a-f]{4}\\].*");
            });
        }

        @Test
        void 같은_세션에서_동일한_이메일은_항상_동일한_토큰으로_마스킹된다() {
            final PiiMaskingSession session = PiiMaskingSession.forSeed(42L);

            final String first = session.mask("from=admin@zzol.site 첫번째");
            final String second = session.mask("to=admin@zzol.site 두번째");

            final String token1 = first.replaceAll(".*\\[(EMAIL_[0-9a-f]{4})\\].*", "$1");
            final String token2 = second.replaceAll(".*\\[(EMAIL_[0-9a-f]{4})\\].*", "$1");
            assertThat(token1).isEqualTo(token2);
        }

        @Test
        void 다른_이메일은_다른_토큰으로_마스킹된다() {
            final PiiMaskingSession session = PiiMaskingSession.forSeed(42L);

            final String masked = session.mask("a@example.com b@other.com");

            final String[] parts = masked.split(" ");
            assertThat(parts[0]).isNotEqualTo(parts[1]);
        }

        @Test
        void 다른_seed로_생성한_세션은_같은_이메일에_다른_토큰을_부여한다() {
            final PiiMaskingSession session1 = PiiMaskingSession.forSeed(1L);
            final PiiMaskingSession session2 = PiiMaskingSession.forSeed(2L);

            final String masked1 = session1.mask("user@example.com");
            final String masked2 = session2.mask("user@example.com");

            assertThat(masked1).isNotEqualTo(masked2);
        }

        @Test
        void 이메일과_IP가_함께_있으면_모두_마스킹된다() {
            final PiiMaskingSession session = PiiMaskingSession.forSeed(42L);
            final String text = "email=admin@zzol.site, ip=10.0.0.1";

            final String masked = session.mask(text);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(masked).doesNotContain("admin@zzol.site");
                softly.assertThat(masked).doesNotContain("10.0.0.1");
                softly.assertThat(masked).contains("[EMAIL_");
                softly.assertThat(masked).contains("[IP_");
            });
        }
    }
}
