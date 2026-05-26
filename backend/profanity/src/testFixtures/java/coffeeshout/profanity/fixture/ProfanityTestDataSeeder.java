package coffeeshout.profanity.fixture;

import coffeeshout.profanity.application.ProfanityFilterService;
import coffeeshout.profanity.domain.Language;
import coffeeshout.profanity.domain.ProfanityWord;
import coffeeshout.profanity.domain.ProfanityWordRepository;
import coffeeshout.profanity.domain.WordSource;
import java.util.List;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("test")
public class ProfanityTestDataSeeder {

    private static final List<ProfanityWord> SEED_WORDS = List.of(
            new ProfanityWord("씨발", Language.KOREAN, WordSource.MANUAL),
            new ProfanityWord("개새끼", Language.KOREAN, WordSource.MANUAL),
            new ProfanityWord("fuck", Language.ENGLISH, WordSource.MANUAL)
    );

    private final ProfanityWordRepository wordRepository;
    private final ProfanityFilterService filterService;

    public ProfanityTestDataSeeder(ProfanityWordRepository wordRepository, ProfanityFilterService filterService) {
        this.wordRepository = wordRepository;
        this.filterService = filterService;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        SEED_WORDS.stream()
                .filter(word -> !wordRepository.existsByWord(word.word()))
                .forEach(wordRepository::save);
        filterService.rebuildTrie();
    }
}
