package coffeeshout.profanity.application;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.profanity.domain.Language;
import coffeeshout.profanity.domain.ProfanityErrorCode;
import coffeeshout.profanity.domain.ProfanityWord;
import coffeeshout.profanity.domain.ProfanityWordRepository;
import coffeeshout.profanity.domain.TextNormalizer;
import coffeeshout.profanity.domain.TrieRefreshNotifier;
import coffeeshout.profanity.domain.WordSource;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfanityWordManagementService {

    private final ProfanityWordRepository wordRepository;
    private final TrieRefreshNotifier trieRefreshNotifier;
    private final TextNormalizer textNormalizer;

    @Transactional
    public boolean add(String rawWord, Language language, WordSource source) {
        final ProfanityWord word = ProfanityWord.of(textNormalizer.normalize(rawWord), language, source);
        final boolean changed = wordRepository.save(word);
        if (changed) {
            afterCommit(trieRefreshNotifier::publish);
            log.info("비속어 등록: word={}, lang={}, source={}", word.word(), language, source);
        }
        return changed;
    }

    @Transactional
    public void deactivate(String rawWord) {
        final String normalized = textNormalizer.normalize(rawWord);
        wordRepository.findByWord(normalized)
                .orElseThrow(() -> new BusinessException(ProfanityErrorCode.WORD_NOT_FOUND, "비속어를 찾을 수 없습니다: " + normalized));
        wordRepository.deactivate(normalized);
        afterCommit(trieRefreshNotifier::publish);
        log.info("비속어 비활성화: word={}", normalized);
    }

    @Transactional
    public void saveAll(List<ProfanityWord> words) {
        words.forEach(wordRepository::save);
        afterCommit(trieRefreshNotifier::publish);
        log.info("비속어 일괄 등록: {}건", words.size());
    }

    public List<ProfanityWord> findAll() {
        return wordRepository.findAll();
    }

    public List<ProfanityWord> findAllActive() {
        return wordRepository.findAllActive();
    }

    @Transactional
    public void operatorAllow(String rawWord) {
        final String normalized = textNormalizer.normalize(rawWord);
        wordRepository.operatorAllow(normalized, Language.detect(normalized));
        afterCommit(trieRefreshNotifier::publish);
        log.info("운영자 허용 처리: word={}", normalized);
    }

    public boolean isOperatorAllowed(String rawWord) {
        return findByWord(rawWord)
                .filter(w -> w.source() == WordSource.OPERATOR_ALLOWED)
                .isPresent();
    }

    public Optional<ProfanityWord> findByWord(String rawWord) {
        return wordRepository.findByWord(textNormalizer.normalize(rawWord));
    }

    @Transactional
    public void activate(String rawWord) {
        final String normalized = textNormalizer.normalize(rawWord);
        wordRepository.findByWord(normalized)
                .orElseThrow(() -> new BusinessException(ProfanityErrorCode.WORD_NOT_FOUND, "비속어를 찾을 수 없습니다: " + normalized));
        wordRepository.activate(normalized);
        afterCommit(trieRefreshNotifier::publish);
        log.info("비속어 활성화: word={}", normalized);
    }

    public Page<ProfanityWord> findAllPaged(String search, Language language, WordSource source, Boolean activeOnly, int page, int size) {
        final String searchTerm = (search == null) ? "" : search.strip();
        return wordRepository.findAllPaged(searchTerm, language, source, activeOnly, PageRequest.of(page, size));
    }

    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
