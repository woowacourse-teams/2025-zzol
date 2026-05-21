package coffeeshout.global.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "outbox_event",
        indexes = {
                @Index(name = "idx_outbox_status_id", columnList = "status, id"),
                @Index(name = "idx_outbox_join_code_status", columnList = "join_code, status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String streamKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(length = 4)
    private String joinCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(nullable = false)
    private int retryCount;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public static OutboxEvent create(String streamKey, String payload) {
        return create(streamKey, payload, null);
    }

    public static OutboxEvent create(String streamKey, String payload, String joinCode) {
        final OutboxEvent event = new OutboxEvent();
        event.streamKey = streamKey;
        event.payload = payload;
        event.joinCode = joinCode;
        event.status = OutboxStatus.PENDING;
        event.retryCount = 0;
        event.createdAt = Instant.now();
        event.updatedAt = event.createdAt;
        return event;
    }

    public void markInProgress() {
        this.status = OutboxStatus.IN_PROGRESS;
        this.updatedAt = Instant.now();
    }

    public void markPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.updatedAt = Instant.now();
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    public void markDeadLetter() {
        this.status = OutboxStatus.DEAD_LETTER;
        this.updatedAt = Instant.now();
    }

    public void setStatusPending() {
        this.status = OutboxStatus.PENDING;
        this.updatedAt = Instant.now();
    }
}
