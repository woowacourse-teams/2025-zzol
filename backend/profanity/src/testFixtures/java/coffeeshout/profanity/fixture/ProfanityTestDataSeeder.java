package coffeeshout.profanity.fixture;

import coffeeshout.profanity.application.ProfanityFilterService;
import coffeeshout.profanity.domain.Language;
import coffeeshout.profanity.domain.ProfanityWord;
import coffeeshout.profanity.domain.ProfanityWordRepository;
import coffeeshout.profanity.domain.WordSource;
import java.util.List;

public class ProfanityTestDataSeeder {

    private static final List<ProfanityWord> SEED_WORDS = List.of(
            new ProfanityWord("씨발", Language.KOREAN, WordSource.MANUAL, true),
            new ProfanityWord("개새끼", Language.KOREAN, WordSource.MANUAL, true),
            new ProfanityWord("fuck", Language.ENGLISH, WordSource.MANUAL, true)
    );

    private final ProfanityWordRepository wordRepository;
    private final ProfanityFilterService filterService;

    public ProfanityTestDataSeeder(ProfanityWordRepository wordRepository, ProfanityFilterService filterService) {
        this.wordRepository = wordRepository;
        this.filterService = filterService;
    }

    public void seedForTest() {
        SEED_WORDS.stream()
                .filter(word -> !wordRepository.existsByWord(word.word()))
                .forEach(wordRepository::save);
        filterService.rebuildTrie();
    }
}
