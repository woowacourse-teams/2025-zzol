package coffeeshout.global.zzolbot.infra;

import coffeeshout.global.outbox.OutboxEvent;
import coffeeshout.global.outbox.OutboxStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ZzolBotOutboxRepository extends JpaRepository<OutboxEvent, Long> {

    @Query("SELECT o FROM OutboxEvent o WHERE o.status IN :statuses AND o.payload LIKE :pattern ORDER BY o.createdAt DESC")
    List<OutboxEvent> findRecentByStatusInAndPayloadContaining(
            @Param("statuses") List<OutboxStatus> statuses,
            @Param("pattern") String pattern
    );
}
