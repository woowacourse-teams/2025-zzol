package coffeeshout.zzolbot.eval.infra;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvalResultRepository extends JpaRepository<EvalResultEntity, Long> {

    List<EvalResultEntity> findByRunIdOrderByIdAsc(Long runId);
}
