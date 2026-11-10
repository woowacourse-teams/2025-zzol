package coffeeshout.profanity.infra.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfanityWordJpaRepository extends JpaRepository<ProfanityWordEntity, Long> {

    Optional<ProfanityWordEntity> findByWord(String word);

    List<ProfanityWordEntity> findAllByWordIn(Collection<String> words);

    boolean existsByWord(String word);
}
