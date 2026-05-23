package coffeeshout.profanity.application;

import coffeeshout.profanity.domain.Language;
import coffeeshout.profanity.domain.ProfanityErrorCode;
import coffeeshout.profanity.domain.ProfanityWord;
import coffeeshout.profanity.domain.ProfanityWordRepository;
import coffeeshout.profanity.domain.WordSource;
import coffeeshout.profanity.infra.redis.ProfanityTrieRefreshPublisher;
import coffeeshout.global.exception.custom.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfanityWordManagementService {

    private final ProfanityWordRepository wordRepository;
    private final ProfanityTrieRefreshPublisher refreshPublisher;

    @Transactional
    public void add(String rawWord, Language language, WordSource source) {
        final ProfanityWord word = new ProfanityWord(rawWord, language, source);
        wordRepository.save(word);
        refreshPublisher.publish();
        log.info("비속어 등록: word={}, lang={}, source={}", word.word(), language, source);
    }

    @Transactional
    public void deactivate(String rawWord) {
        final String normalized = rawWord == null ? "" : rawWord.trim().toLowerCase();
        wordRepository.findByWord(normalized)
                .orElseThrow(() -> new BusinessException(ProfanityErrorCode.WORD_NOT_FOUND, "비속어를 찾을 수 없습니다: " + normalized));
        wordRepository.deactivate(normalized);
        refreshPublisher.publish();
        log.info("비속어 비활성화: word={}", normalized);
    }

    @Transactional
    public void saveAll(List<ProfanityWord> words) {
        words.forEach(wordRepository::save);
        refreshPublisher.publish();
        log.info("비속어 일괄 등록: {}건", words.size());
    }

    public List<ProfanityWord> findAll() {
        return wordRepository.findAll();
    }

    public List<ProfanityWord> findAllActive() {
        return wordRepository.findAllActive();
    }
}
