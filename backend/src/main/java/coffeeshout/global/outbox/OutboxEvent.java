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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "outbox_event",
        indexes = @Index(name = "idx_outbox_status_id", columnList = "status, id")
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(nullable = false)
    private int retryCount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static OutboxEvent create(String streamKey, String payload) {
        final OutboxEvent event = new OutboxEvent();
        event.streamKey = streamKey;
        event.payload = payload;
        event.status = OutboxStatus.PENDING;
        event.retryCount = 0;
        event.createdAt = LocalDateTime.now();
        return event;
    }

    public void markInProgress() {
        this.status = OutboxStatus.IN_PROGRESS;
    }

    public void markPublished() {
        this.status = OutboxStatus.PUBLISHED;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    public void markDeadLetter() {
        this.status = OutboxStatus.DEAD_LETTER;
    }

    public void setStatusPending() {
        this.status = OutboxStatus.PENDING;
    }
}
