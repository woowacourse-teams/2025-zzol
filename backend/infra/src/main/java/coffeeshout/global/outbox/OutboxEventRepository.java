package coffeeshout.global.outbox;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
            nativeQuery = true)
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

    /**
     * 상태별 누적 건수. DEAD_LETTER 적체 깊이를 Prometheus 게이지로 노출하는 데 쓴다(ADR-0032).
     * {@code idx_outbox_status_id(status, id)} 인덱스를 타므로 스크레이프 시점 COUNT가 가볍다.
     */
    long countByStatus(OutboxStatus status);

    /**
     * 상태별 목록. 백오피스의 격리 메시지 화면이 쓴다.
     *
     * <p>{@code idx_outbox_status_id(status, id)} 를 그대로 탄다. 적체가 깊어져도
     * 첫 페이지를 여는 비용이 늘지 않는다.
     */
    Page<OutboxEvent> findByStatus(OutboxStatus status, Pageable pageable);
}
