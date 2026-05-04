package coffeeshout.global.zzolbot.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PiiMaskerTest {

    private PiiMasker piiMasker;

    @BeforeEach
    void setUp() {
        piiMasker = new PiiMasker();
    }

    @Nested
    class mask_메서드 {

        @Test
        void null_입력_시_null을_반환한다() {
            assertThat(piiMasker.mask(null)).isNull();
        }

        @Test
        void 마스킹_대상이_없으면_원문을_반환한다() {
            final String text = "joinCode=ABC123, 게임 상태 정상";

            assertThat(piiMasker.mask(text)).isEqualTo(text);
        }

        @Test
        void 이메일_주소를_마스킹한다() {
            final String text = "사용자 이메일: user@example.com";

            assertThat(piiMasker.mask(text)).isEqualTo("사용자 이메일: [EMAIL]");
        }

        @Test
        void IP_주소를_마스킹한다() {
            final String text = "클라이언트 IP: 192.168.1.100에서 접속";

            assertThat(piiMasker.mask(text)).isEqualTo("클라이언트 IP: [IP]에서 접속");
        }

        @Test
        void 이메일과_IP가_함께_있으면_모두_마스킹한다() {
            final String text = "email=admin@zzol.site, ip=10.0.0.1";

            SoftAssertions.assertSoftly(softly -> {
                final String masked = piiMasker.mask(text);
                softly.assertThat(masked).doesNotContain("admin@zzol.site");
                softly.assertThat(masked).doesNotContain("10.0.0.1");
                softly.assertThat(masked).contains("[EMAIL]");
                softly.assertThat(masked).contains("[IP]");
            });
        }

        @Test
        void 텍스트에_포함된_여러_이메일을_모두_마스킹한다() {
            final String text = "from=sender@a.com, to=receiver@b.com";

            final String masked = piiMasker.mask(text);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(masked).doesNotContain("sender@a.com");
                softly.assertThat(masked).doesNotContain("receiver@b.com");
                softly.assertThat(masked).isEqualTo("from=[EMAIL], to=[EMAIL]");
            });
        }

        @Test
        void 루프백_및_사설_IP도_마스킹한다() {
            final String text = "127.0.0.1 및 192.168.0.1 접속";

            SoftAssertions.assertSoftly(softly -> {
                final String masked = piiMasker.mask(text);
                softly.assertThat(masked).doesNotContain("127.0.0.1");
                softly.assertThat(masked).doesNotContain("192.168.0.1");
            });
        }
    }
}
