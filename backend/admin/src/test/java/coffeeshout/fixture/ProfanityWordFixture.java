package coffeeshout.fixture;

import coffeeshout.profanity.domain.Language;
import coffeeshout.profanity.domain.ProfanityWord;
import coffeeshout.profanity.domain.WordSource;

public final class ProfanityWordFixture {

    public static ProfanityWord 한국어_수동_욕설() {
        return new ProfanityWord("욕설", Language.KOREAN, WordSource.MANUAL);
    }

    public static ProfanityWord 영어_LDNOOBW_욕설() {
        return new ProfanityWord("badword", Language.ENGLISH, WordSource.LDNOOBW);
    }
}
