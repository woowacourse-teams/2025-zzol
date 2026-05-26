package coffeeshout.profanity.domain;

import coffeeshout.global.exception.custom.BusinessException;

public record ProfanityWord(String word, Language language, WordSource source, boolean isActive) {

    public static final int MAX_WORD_LENGTH = 200;

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
        if (language == null) {
            throw new BusinessException(ProfanityErrorCode.LANGUAGE_REQUIRED, "language는 null일 수 없습니다.");
        }
        if (source == null) {
            throw new BusinessException(ProfanityErrorCode.SOURCE_REQUIRED, "source는 null일 수 없습니다.");
        }
    }
}
