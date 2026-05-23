package coffeeshout.profanity.infra.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProfanityWordJpaRepository extends JpaRepository<ProfanityWordEntity, Long> {

    @Query("SELECT e FROM ProfanityWordEntity e WHERE e.isActive = true")
    List<ProfanityWordEntity> findAllActive();

    Optional<ProfanityWordEntity> findByWord(String word);

    boolean existsByWord(String word);
}
