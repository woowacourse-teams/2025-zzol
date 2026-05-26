package coffeeshout.fixture;

import coffeeshout.profanity.domain.Language;
import coffeeshout.profanity.domain.ProfanityWord;
import coffeeshout.profanity.domain.WordSource;

public final class ProfanityWordFixture {

    private ProfanityWordFixture() {
    }

    public static ProfanityWord 한국어_수동_욕설() {
        return new ProfanityWord("욕설", Language.KOREAN, WordSource.MANUAL, true);
    }

    public static ProfanityWord 영어_LDNOOBW_욕설() {
        return new ProfanityWord("badword", Language.ENGLISH, WordSource.LDNOOBW, true);
    }

    public static ProfanityWord 한국어_AI_FLAGGED_욕설() {
        return new ProfanityWord("욕설닉네임", Language.KOREAN, WordSource.AI_FLAGGED, true);
    }

    public static ProfanityWord 한국어_VANE_욕설() {
        return new ProfanityWord("비속어", Language.KOREAN, WordSource.VANE, true);
    }

    public static ProfanityWord 운영자_허용_단어() {
        return new ProfanityWord("허용닉네임", Language.KOREAN, WordSource.OPERATOR_ALLOWED, true);
    }

    public static String 최대_길이_초과_단어() {
        return "가".repeat(ProfanityWord.MAX_WORD_LENGTH + 1);
    }
}
