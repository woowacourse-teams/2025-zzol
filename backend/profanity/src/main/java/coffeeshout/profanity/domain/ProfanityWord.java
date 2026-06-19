package coffeeshout.profanity.domain;

import coffeeshout.global.exception.custom.BusinessException;

public record ProfanityWord(String word, Language language, WordSource source, boolean isActive) {

    public static final int MAX_WORD_LENGTH = 200;
    public static final int MIN_ASCII_WORD_LENGTH = 3;

    public ProfanityWord {
        validate(word, language, source);
    }

    public static ProfanityWord of(String word, Language language, WordSource source) {
        return new ProfanityWord(word, language, source, true);
    }

    private static void validate(String word, Language language, WordSource source) {
        if (word == null || word.isBlank()) {
            throw new BusinessException(ProfanityErrorCode.WORD_BLANK,
                    "비속어는 공백일 수 없습니다. 입력값: '" + word + "'");
        }
        if (word.length() > MAX_WORD_LENGTH) {
            throw new BusinessException(ProfanityErrorCode.WORD_TOO_LONG,
                    "비속어는 " + MAX_WORD_LENGTH + "자 이하여야 합니다. 현재 길이: " + word.length());
        }
        if (isDegenerateAsciiKeyword(word)) {
            throw new BusinessException(ProfanityErrorCode.WORD_TOO_SHORT,
                    "ASCII 비속어는 " + MIN_ASCII_WORD_LENGTH + "자 이상이어야 합니다. "
                            + "너무 짧은 ASCII 키워드는 무관한 닉네임을 부분 매칭으로 오탐한다. 입력값: '" + word + "'");
        }
        if (language == null) {
            throw new BusinessException(ProfanityErrorCode.LANGUAGE_REQUIRED, "language는 null일 수 없습니다.");
        }
        if (source == null) {
            throw new BusinessException(ProfanityErrorCode.SOURCE_REQUIRED, "source는 null일 수 없습니다.");
        }
    }

    /**
     * 정규화 후 ASCII 문자만으로 구성된 {@value #MIN_ASCII_WORD_LENGTH}자 미만 키워드를 차단한다.
     *
     * <p>{@code @!@} 같은 기호 항목이 정규화(리트 치환 {@code @}→{@code a}, 특수문자 제거)를 거치며
     * {@code aa}로 붕괴해, 닉네임에 해당 문자열이 부분 포함되기만 해도 차단되는 오탐을 막는다.
     * 한글은 단일 문자가 ASCII 범위를 벗어나므로 {@code 씨발}(2자) 같은 짧은 한글 단어는 그대로 허용된다.
     */
    private static boolean isDegenerateAsciiKeyword(String word) {
        if (word.length() >= MIN_ASCII_WORD_LENGTH) {
            return false;
        }
        return word.chars().allMatch(c -> c < 128);
    }
}
