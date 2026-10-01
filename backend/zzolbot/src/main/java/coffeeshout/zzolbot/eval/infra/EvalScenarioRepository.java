package coffeeshout.zzolbot.eval.infra;

import coffeeshout.zzolbot.eval.domain.ScenarioKind;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvalScenarioRepository extends JpaRepository<EvalScenarioEntity, Long> {

    List<EvalScenarioEntity> findAllByOrderByCreatedAtDesc();

    List<EvalScenarioEntity> findAllByKindOrderByCreatedAtDesc(ScenarioKind kind);

    Optional<EvalScenarioEntity> findByName(String name);

    boolean existsByName(String name);
}
