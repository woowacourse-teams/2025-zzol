package coffeeshout.bombrelay.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class KoreanCharUtilsTest {

    @Nested
    class 마지막_글자_추출 {

        @Test
        void 단어의_마지막_글자를_추출한다() {
            assertThat(KoreanCharUtils.getLastChar("사과")).isEqualTo('과');
            assertThat(KoreanCharUtils.getLastChar("컴퓨터")).isEqualTo('터');
        }
    }

    @Nested
    class 첫_글자_추출 {

        @Test
        void 단어의_첫_글자를_추출한다() {
            assertThat(KoreanCharUtils.getFirstChar("과자")).isEqualTo('과');
            assertThat(KoreanCharUtils.getFirstChar("터널")).isEqualTo('터');
        }
    }

    @Nested
    class 끝말잇기_첫글자_일치_확인 {

        @Test
        void 이전_단어_마지막_글자와_다음_단어_첫_글자가_같으면_true() {
            assertThat(KoreanCharUtils.isValidFirstChar('과', '과')).isTrue();
        }

        @Test
        void 이전_단어_마지막_글자와_다음_단어_첫_글자가_다르면_false() {
            assertThat(KoreanCharUtils.isValidFirstChar('과', '사')).isFalse();
        }

        @Test
        void 두음법칙_녀를_여로_변환() {
            // "녀" → "여" (예: 여자)
            assertThat(KoreanCharUtils.isValidFirstChar('녀', '여')).isTrue();
        }

        @Test
        void 두음법칙_리를_이로_변환() {
            // "리" → "이" (예: 이발)
            assertThat(KoreanCharUtils.isValidFirstChar('리', '이')).isTrue();
        }

        @Test
        void 두음법칙_류를_유로_변환() {
            // "류" → "유" (예: 유행)
            assertThat(KoreanCharUtils.isValidFirstChar('류', '유')).isTrue();
        }

        @Test
        void 두음법칙_라를_나로_변환() {
            // "라" → "나" (예: 나무)
            assertThat(KoreanCharUtils.isValidFirstChar('라', '나')).isTrue();
        }

        @Test
        void 두음법칙_원래_글자도_허용() {
            // "녀"로 끝나면 "녀"로 시작하는 단어도 허용
            assertThat(KoreanCharUtils.isValidFirstChar('녀', '녀')).isTrue();
        }

        @Test
        void 두음법칙_해당_없는_글자는_변환하지_않는다() {
            assertThat(KoreanCharUtils.isValidFirstChar('가', '나')).isFalse();
        }
    }

    @Nested
    class 한국어_판별 {

        @Test
        void 한국어_단어는_true() {
            assertThat(KoreanCharUtils.isKorean("사과")).isTrue();
            assertThat(KoreanCharUtils.isKorean("컴퓨터")).isTrue();
        }

        @Test
        void 영어가_포함되면_false() {
            assertThat(KoreanCharUtils.isKorean("abc")).isFalse();
            assertThat(KoreanCharUtils.isKorean("사과a")).isFalse();
        }
    }
}
