package coffeeshout.global.outbox;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query(
            value = "SELECT * FROM outbox_event "
                    + "WHERE status = 'PENDING' "
                    + "ORDER BY id ASC "
                    + "LIMIT :size "
                    + "FOR UPDATE SKIP LOCKED",
            nativeQuery = true
    )
    List<OutboxEvent> findPendingEventsForUpdate(@Param("size") int size);

    /**
     * IN_PROGRESS 상태로 오래 남아있는 이벤트를 복구한다.
     * 서버가 IN_PROGRESS 상태에서 죽었을 때를 대비한 안전장치.
     */
    @Modifying
    @Query("UPDATE OutboxEvent o SET o.status = 'PENDING', o.updatedAt = CURRENT_TIMESTAMP "
            + "WHERE o.status = 'IN_PROGRESS' AND o.updatedAt < :threshold")
    int recoverStaleInProgressEvents(@Param("threshold") LocalDateTime threshold);

    @Modifying
    @Query("DELETE FROM OutboxEvent o WHERE o.status = 'PUBLISHED' AND o.createdAt < :threshold")
    int deletePublishedEventsBefore(@Param("threshold") LocalDateTime threshold);
}
