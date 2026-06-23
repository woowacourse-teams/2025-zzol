package coffeeshout.zzolbot.eval.infra;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvalScenarioRepository extends JpaRepository<EvalScenarioEntity, Long> {

    List<EvalScenarioEntity> findAllByOrderByCreatedAtDesc();

    Optional<EvalScenarioEntity> findByName(String name);

    boolean existsByName(String name);
}
