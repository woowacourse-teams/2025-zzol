package coffeeshout.profanity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TextNormalizerTest {

    private TextNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new TextNormalizer();
    }

    @Nested
    class null_또는_빈_입력 {

        @Test
        void null이면_빈_문자열을_반환한다() {
            assertThat(normalizer.normalize(null)).isEmpty();
        }

        @Test
        void 빈_문자열은_빈_문자열을_반환한다() {
            assertThat(normalizer.normalize("")).isEmpty();
        }
    }

    @Nested
    class 대소문자_정규화 {

        @Test
        void 대문자는_소문자로_변환된다() {
            assertThat(normalizer.normalize("BADWORD")).isEqualTo("badword");
        }

        @Test
        void 한글은_영향받지_않는다() {
            assertThat(normalizer.normalize("욕설")).isEqualTo("욕설");
        }
    }

    @Nested
    class 특수문자_제거 {

        @Test
        void 특수문자가_제거된다() {
            assertThat(normalizer.normalize("b!d#w%r^d")).isEqualTo("bdwrd");
        }

        @Test
        void 공백이_제거된다() {
            assertThat(normalizer.normalize("bad word")).isEqualTo("badword");
        }

        @Test
        void 한글_사이_특수문자가_제거된다() {
            assertThat(normalizer.normalize("욕!설")).isEqualTo("욕설");
        }
    }

    @Nested
    class 유니코드_호환_문자_정규화 {

        @Test
        void 원문자_소문자는_일반_알파벳으로_변환된다() {
            assertThat(normalizer.normalize("ⓑⓞⓩⓘ")).isEqualTo("bozi");
        }

        @Test
        void 괄호_알파벳은_일반_알파벳으로_변환된다() {
            assertThat(normalizer.normalize("⒮⒠⒳")).isEqualTo("sex");
        }

        @Test
        void 원문자와_일반_문자_혼합도_정규화된다() {
            assertThat(normalizer.normalize("SⓔX")).isEqualTo("sex");
        }

        @Test
        void 원문자_대문자는_소문자로_변환된다() {
            assertThat(normalizer.normalize("ⓢEX")).isEqualTo("sex");
        }
    }

    @Nested
    class 리트스피크_치환 {

        @Test
        void 숫자_0은_o로_치환된다() {
            assertThat(normalizer.normalize("f0ck")).isEqualTo("fock");
        }

        @Test
        void 숫자_1은_i로_치환된다() {
            assertThat(normalizer.normalize("sh1t")).isEqualTo("shit");
        }

        @Test
        void 숫자_3은_e로_치환된다() {
            assertThat(normalizer.normalize("s3x")).isEqualTo("sex");
        }

        @Test
        void 숫자_4는_a로_치환된다() {
            assertThat(normalizer.normalize("b4d")).isEqualTo("bad");
        }

        @Test
        void 숫자_5는_s로_치환된다() {
            assertThat(normalizer.normalize("5hit")).isEqualTo("shit");
        }

        @Test
        void 숫자_7은_t로_치환된다() {
            assertThat(normalizer.normalize("shi7")).isEqualTo("shit");
        }

        @Test
        void 달러_기호는_s로_치환된다() {
            assertThat(normalizer.normalize("$hit")).isEqualTo("shit");
        }

        @Test
        void at_기호는_a로_치환된다() {
            assertThat(normalizer.normalize("b@d")).isEqualTo("bad");
        }

        @Test
        void 복합_리트스피크_우회가_감지된다() {
            assertThat(normalizer.normalize("$h1T")).isEqualTo("shit");
        }
    }
}
