package coffeeshout.admin.profanity.ui.response;

import coffeeshout.profanity.domain.Language;
import coffeeshout.profanity.domain.ProfanityWord;
import coffeeshout.profanity.domain.WordSource;

public record ProfanityWordResponse(String word, Language language, WordSource source, boolean active) {

    public static ProfanityWordResponse from(ProfanityWord word) {
        return new ProfanityWordResponse(word.word(), word.language(), word.source(), word.isActive());
    }
}
