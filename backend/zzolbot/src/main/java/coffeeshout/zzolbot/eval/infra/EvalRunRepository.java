package coffeeshout.zzolbot.eval.infra;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvalRunRepository extends JpaRepository<EvalRunEntity, Long> {

    List<EvalRunEntity> findTop20ByOrderByStartedAtDesc();
}
