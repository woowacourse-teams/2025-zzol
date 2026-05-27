package coffeeshout.profanity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LanguageTest {

    @Nested
    class detect_언어_감지 {

        @ParameterizedTest
        @ValueSource(strings = {"욕설", "씨발", "ㅅㅂ", "s이발", "욕설abc"})
        void 한글_문자가_포함되면_KOREAN을_반환한다(String text) {
            assertThat(Language.detect(text)).isEqualTo(Language.KOREAN);
        }

        @ParameterizedTest
        @ValueSource(strings = {"badword", "FUCK", "asshole", "123abc", "b@dw0rd"})
        void 한글이_없으면_ENGLISH를_반환한다(String text) {
            assertThat(Language.detect(text)).isEqualTo(Language.ENGLISH);
        }

        @Test
        void 자모_분리_우회_닉네임은_KOREAN을_반환한다() {
            assertThat(Language.detect("ㅅㅂㄹㅁ")).isEqualTo(Language.KOREAN);
        }

        @Test
        void 빈_문자열은_ENGLISH를_반환한다() {
            assertThat(Language.detect("")).isEqualTo(Language.ENGLISH);
        }
    }
}
