package coffeeshout.profanity.infra.persistence;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfanityWordJpaRepository extends JpaRepository<ProfanityWordEntity, Long> {

    @Query("SELECT e FROM ProfanityWordEntity e WHERE e.isActive = true AND e.source != 'OPERATOR_ALLOWED'")
    List<ProfanityWordEntity> findAllActive();

    @Query("SELECT e.word FROM ProfanityWordEntity e WHERE e.isActive = true AND e.source != 'OPERATOR_ALLOWED' AND e.word IN :candidates")
    Set<String> findAllActiveIn(@Param("candidates") Set<String> candidates);

    Optional<ProfanityWordEntity> findByWord(String word);

    boolean existsByWord(String word);
}
