package coffeeshout.profanity.infra.persistence;

import coffeeshout.profanity.domain.ProfanityWord;
import coffeeshout.profanity.domain.ProfanityWordRepository;
import java.util.List;
import java.util.Optional;
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
    public boolean existsByWord(String word) {
        return jpaRepository.existsByWord(word);
    }

    @Override
    public void save(ProfanityWord word) {
        jpaRepository.findByWord(word.word())
                .ifPresentOrElse(
                        entity -> entity.reactivate(),
                        () -> jpaRepository.save(ProfanityWordEntity.from(word))
                );
    }

    @Override
    public void deactivate(String word) {
        jpaRepository.findByWord(word).ifPresent(ProfanityWordEntity::deactivate);
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
