package coffeeshout.profanity.infra.persistence;

import coffeeshout.profanity.domain.Language;
import coffeeshout.profanity.domain.ProfanityWord;
import coffeeshout.profanity.domain.ProfanityWordRepository;
import coffeeshout.profanity.domain.WordSource;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProfanityWordRepositoryImpl implements ProfanityWordRepository {

    private final ProfanityWordJpaRepository jpaRepository;

    @Override
    public List<ProfanityWord> findAllActive() {
        return jpaRepository.findAllActive().stream()
                .map(ProfanityWordEntity::toDomain)
                .toList();
    }

    @Override
    public Set<String> findAllActiveIn(Set<String> candidates) {
        return jpaRepository.findAllActiveIn(candidates);
    }

    @Override
    public boolean existsByWord(String word) {
        return jpaRepository.existsByWord(word);
    }

    @Override
    public boolean save(ProfanityWord word) {
        final Optional<ProfanityWordEntity> existing = jpaRepository.findByWord(word.word());
        if (existing.isPresent()) {
            final ProfanityWordEntity entity = existing.get();
            if (entity.getSource() == WordSource.OPERATOR_ALLOWED) {
                // 운영자 명시 허용 단어는 MANUAL 재차단만 허용, AI_FLAGGED 등은 무시
                if (word.source() == WordSource.MANUAL) {
                    entity.overrideSource(WordSource.MANUAL);
                    return true;
                }
                return false;
            }
            return entity.reactivate();
        }
        jpaRepository.save(ProfanityWordEntity.from(word));
        return true;
    }

    @Override
    public void deactivate(String word) {
        jpaRepository.findByWord(word).ifPresent(ProfanityWordEntity::deactivate);
    }

    @Override
    public void operatorAllow(String word, Language language) {
        jpaRepository.findByWord(word)
                .ifPresentOrElse(
                        ProfanityWordEntity::operatorAllow,
                        () -> jpaRepository.save(ProfanityWordEntity.fromOperatorAllowed(word, language))
                );
    }

    @Override
    public Optional<ProfanityWord> findByWord(String word) {
        return jpaRepository.findByWord(word).map(ProfanityWordEntity::toDomain);
    }

    @Override
    public List<ProfanityWord> findAll() {
        return jpaRepository.findAll().stream()
                .map(ProfanityWordEntity::toDomain)
                .toList();
    }
}
