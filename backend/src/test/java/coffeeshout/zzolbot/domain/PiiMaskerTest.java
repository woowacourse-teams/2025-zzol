package coffeeshout.zzolbot.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PiiMaskerTest {

    private PiiMasker piiMasker;
    private PiiMaskingSession session;

    @BeforeEach
    void setUp() {
        piiMasker = new PiiMasker();
        session = PiiMaskingSession.forSeed(42L);
    }

    @Nested
    class mask_메서드 {

        @Test
        void null_입력_시_null을_반환한다() {
            assertThat(piiMasker.mask(null, session)).isNull();
        }

        @Test
        void 마스킹_대상이_없으면_원문을_반환한다() {
            final String text = "joinCode=ABC123, 게임 상태 정상";

            assertThat(piiMasker.mask(text, session)).isEqualTo(text);
        }

        @Test
        void 이메일_주소를_토큰으로_마스킹한다() {
            final String text = "사용자 이메일: user@example.com";

            final String masked = piiMasker.mask(text, session);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(masked).doesNotContain("user@example.com");
                softly.assertThat(masked).contains("[EMAIL_");
            });
        }

        @Test
        void IP_주소를_토큰으로_마스킹한다() {
            final String text = "클라이언트 IP: 192.168.1.100에서 접속";

            final String masked = piiMasker.mask(text, session);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(masked).doesNotContain("192.168.1.100");
                softly.assertThat(masked).contains("[IP_");
            });
        }

        @Test
        void 이메일과_IP가_함께_있으면_모두_마스킹한다() {
            final String text = "email=admin@zzol.site, ip=10.0.0.1";

            final String masked = piiMasker.mask(text, session);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(masked).doesNotContain("admin@zzol.site");
                softly.assertThat(masked).doesNotContain("10.0.0.1");
                softly.assertThat(masked).contains("[EMAIL_");
                softly.assertThat(masked).contains("[IP_");
            });
        }

        @Test
        void 같은_세션에서_동일한_이메일은_동일한_토큰으로_마스킹된다() {
            final String text1 = "email=admin@zzol.site";
            final String text2 = "from=admin@zzol.site";

            final String token1 = piiMasker.mask(text1, session).replaceAll(".*\\[EMAIL_(\\w+)\\].*", "$1");
            final String token2 = piiMasker.mask(text2, session).replaceAll(".*\\[EMAIL_(\\w+)\\].*", "$1");

            assertThat(token1).isEqualTo(token2);
        }

        @Test
        void 루프백_및_사설_IP도_마스킹한다() {
            final String text = "127.0.0.1 및 192.168.0.1 접속";

            final String masked = piiMasker.mask(text, session);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(masked).doesNotContain("127.0.0.1");
                softly.assertThat(masked).doesNotContain("192.168.0.1");
            });
        }
    }
}
