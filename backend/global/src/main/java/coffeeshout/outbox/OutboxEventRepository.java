package coffeeshout.outbox;

import java.time.Instant;
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
     * CURRENT_TIMESTAMP는 Hibernate 6에서 java.sql.Timestamp로 해석되어
     * Instant 필드에 할당할 수 없으므로, 파라미터로 Instant.now()를 전달한다.
     */
    @Modifying
    @Query("UPDATE OutboxEvent o SET o.status = 'PENDING', o.updatedAt = :now "
            + "WHERE o.status = 'IN_PROGRESS' AND o.updatedAt < :threshold")
    int recoverStaleInProgressEvents(@Param("threshold") Instant threshold, @Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM OutboxEvent o WHERE o.status = 'PUBLISHED' AND o.createdAt < :threshold")
    int deletePublishedEventsBefore(@Param("threshold") Instant threshold);
}
