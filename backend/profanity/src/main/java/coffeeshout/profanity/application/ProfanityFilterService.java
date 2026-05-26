package coffeeshout.profanity.application;

import coffeeshout.global.nickname.ProfanityChecker;
import coffeeshout.profanity.domain.ProfanityWord;
import coffeeshout.profanity.domain.ProfanityWordRepository;
import coffeeshout.profanity.domain.TextNormalizer;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ahocorasick.trie.Trie;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfanityFilterService implements ProfanityChecker {

    private final AtomicReference<Trie> trieRef = new AtomicReference<>(Trie.builder().build());

    private final ProfanityWordRepository wordRepository;
    private final TextNormalizer textNormalizer;

    @PostConstruct
    public void init() {
        rebuildTrie();
    }

    @Override
    public boolean contains(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        final String normalized = textNormalizer.normalize(text);
        return trieRef.get().containsMatch(normalized);
    }

    public void rebuildTrie() {
        final List<ProfanityWord> words = wordRepository.findAllActive();
        final Trie.TrieBuilder builder = Trie.builder().ignoreOverlaps().ignoreCase();
        words.forEach(w -> builder.addKeyword(textNormalizer.normalize(w.word())));
        trieRef.set(builder.build());
        log.info("비속어 트라이 재구성 완료 — {}건", words.size());
    }
}
