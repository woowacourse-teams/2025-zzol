package coffeeshout.outbox;

public enum OutboxStatus {
    PENDING,
    IN_PROGRESS,
    PUBLISHED,
    DEAD_LETTER
}
