package coffeeshout.bombrelay.domain;

import java.util.Map;

/**
 * 한글 문자 처리 유틸.
 * - 마지막 글자 추출
 * - 두음법칙 변환
 * - 첫 글자 일치 확인 (두음법칙 적용)
 */
public final class KoreanCharUtils {

    private static final int HANGUL_BASE = 0xAC00;
    private static final int INITIAL_COUNT = 19;
    private static final int MEDIAL_COUNT = 21;
    private static final int FINAL_COUNT = 28;

    private static final Map<Character, Character> DUEUM_MAP = Map.ofEntries(
            Map.entry('녀', '여'), Map.entry('뇨', '요'), Map.entry('뉴', '유'), Map.entry('니', '이'),
            Map.entry('랴', '야'), Map.entry('려', '여'), Map.entry('례', '예'), Map.entry('료', '요'),
            Map.entry('류', '유'), Map.entry('리', '이'), Map.entry('라', '나'), Map.entry('래', '내'),
            Map.entry('로', '노'), Map.entry('뢰', '뇌'), Map.entry('루', '누'), Map.entry('르', '느')
    );

    private KoreanCharUtils() {
    }

    public static char getLastChar(String word) {
        return word.charAt(word.length() - 1);
    }

    public static char getFirstChar(String word) {
        return word.charAt(0);
    }

    public static boolean isValidFirstChar(char previousLastChar, char currentFirstChar) {
        if (previousLastChar == currentFirstChar) {
            return true;
        }
        // 두음법칙: 이전 단어 마지막 글자를 두음법칙 변환한 결과가 현재 단어 첫 글자와 같은지
        final char converted = applyDueum(previousLastChar);
        return converted == currentFirstChar;
    }

    public static char applyDueum(char ch) {
        return DUEUM_MAP.getOrDefault(ch, ch);
    }

    public static boolean isKorean(String word) {
        return word.chars().allMatch(ch -> ch >= HANGUL_BASE && ch < HANGUL_BASE + INITIAL_COUNT * MEDIAL_COUNT * FINAL_COUNT);
    }
}
