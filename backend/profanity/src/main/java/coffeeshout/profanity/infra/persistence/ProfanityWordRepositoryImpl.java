package coffeeshout.profanity.infra.persistence;

import coffeeshout.profanity.domain.Language;
import coffeeshout.profanity.domain.ProfanityWord;
import coffeeshout.profanity.domain.ProfanityWordRepository;
import coffeeshout.profanity.domain.WordSource;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProfanityWordRepositoryImpl implements ProfanityWordRepository {

    private final ProfanityWordJpaRepository jpaRepository;
    private final ProfanityWordQueryRepository queryRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<ProfanityWord> findAllActive() {
        return queryRepository.findAllActive().stream()
                .map(ProfanityWordEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsByWord(String word) {
        return jpaRepository.existsByWord(word);
    }

    @Override
    public boolean save(ProfanityWord word) {
        final Optional<ProfanityWordEntity> existing = jpaRepository.findByWord(word.word());
        if (existing.isEmpty()) {
            jpaRepository.save(ProfanityWordEntity.from(word));
            return true;
        }
        return saveExisting(existing.get(), word);
    }

    private boolean saveExisting(ProfanityWordEntity entity, ProfanityWord word) {
        if (entity.getSource() != WordSource.OPERATOR_ALLOWED) {
            return entity.reactivate();
        }
        // 운영자 명시 허용 단어는 MANUAL 재차단만 허용, AI_FLAGGED 등은 무시
        if (word.source() != WordSource.MANUAL) {
            return false;
        }
        entity.overrideSource(WordSource.MANUAL);
        return true;
    }

    @Override
    public int bulkInsertIgnore(List<ProfanityWord> words) {
        if (words.isEmpty()) {
            return 0;
        }
        final Instant now = Instant.now();
        final String sql = "INSERT IGNORE INTO profanity_word (word, language, source, is_active, created_at, updated_at) VALUES (?, ?, ?, true, ?, ?)";
        final int[][] counts = jdbcTemplate.batchUpdate(sql, words, 500, (ps, word) -> {
            ps.setString(1, word.word());
            ps.setString(2, word.language().name());
            ps.setString(3, word.source().name());
            ps.setTimestamp(4, Timestamp.from(now));
            ps.setTimestamp(5, Timestamp.from(now));
        });
        return Arrays.stream(counts).mapToInt(batch -> Arrays.stream(batch).sum()).sum();
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

    @Override
    public Page<ProfanityWord> findAllPaged(String search, Language language, WordSource source, Boolean activeOnly, Pageable pageable) {
        return queryRepository.findAllPaged(search, language, source, activeOnly, pageable)
                .map(ProfanityWordEntity::toDomain);
    }

    @Override
    public void activate(String word) {
        jpaRepository.findByWord(word).ifPresent(ProfanityWordEntity::reactivate);
    }
}
