package coffeeshout.zzolbot.infra;

import coffeeshout.global.outbox.OutboxEvent;
import coffeeshout.global.outbox.OutboxStatus;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ZzolBotOutboxRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findByJoinCodeAndStatusInOrderByCreatedAtDesc(
            String joinCode,
            List<OutboxStatus> statuses,
            Pageable pageable
    );

    long countByStatusIn(List<OutboxStatus> statuses);
}
