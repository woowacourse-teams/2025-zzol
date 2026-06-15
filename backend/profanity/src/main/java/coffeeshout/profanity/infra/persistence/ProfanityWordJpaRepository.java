package coffeeshout.profanity.infra.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfanityWordJpaRepository extends JpaRepository<ProfanityWordEntity, Long> {

    Optional<ProfanityWordEntity> findByWord(String word);

    boolean existsByWord(String word);
}
