package coffeeshout.settlement.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 정산 DLQ. 최대 재전달 횟수를 소진했거나 파싱 불가(포이즌)한 메시지를 격리 보관한다.
 * <p>
 * Redis 스트림이 아니라 RDBMS에 두는 이유(리뷰 #1612): 격리 메시지는 재처리가 아니라
 * <b>장기 보존과 사후 분석</b>이 목적인데, Redis는 메모리를 점유하고 eviction 정책에 따라
 * 유실될 수 있다. Outbox의 DEAD_LETTER가 RDBMS에 있는 것과 같은 철학이다.
 */
@Entity
@Table(
        name = "settlement_dead_letter",
        uniqueConstraints = @UniqueConstraint(name = "uk_settlement_dead_letter_record", columnNames = "record_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementDeadLetterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "record_id", nullable = false, length = 64)
    private String recordId;

    @Column(nullable = false, length = 500)
    private String reason;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public SettlementDeadLetterEntity(String recordId, String reason, String payload) {
        this.recordId = recordId;
        this.reason = reason;
        this.payload = payload;
        this.createdAt = LocalDateTime.now();
    }
}
