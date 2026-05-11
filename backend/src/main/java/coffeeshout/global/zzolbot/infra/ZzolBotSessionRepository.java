package coffeeshout.global.zzolbot.infra;

import coffeeshout.global.zzolbot.domain.ZzolBotFeedback;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ZzolBotSessionRepository extends JpaRepository<ZzolBotSessionEntity, Long> {

    List<ZzolBotSessionEntity> findTop20ByOrderByCreatedAtDesc();

    List<ZzolBotSessionEntity> findByFeedbackOrderByCreatedAtDesc(ZzolBotFeedback feedback, Pageable pageable);
}
