package coffeeshout.zzolbot.infra;

import coffeeshout.outbox.OutboxEvent;
import coffeeshout.outbox.OutboxStatus;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ZzolBotOutboxRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findByJoinCodeAndStatusInOrderByCreatedAtDesc(
            String joinCode,
            List<OutboxStatus> statuses,
            Pageable pageable
    );
}
