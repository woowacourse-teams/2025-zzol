package coffeeshout.global.outbox;

/**
 * Outbox 레코드가 저장되었음을 알리는 Spring 내부 이벤트.
 * <p>
 * {@link OutboxEventRecorder}가 Outbox 테이블에 레코드를 저장한 직후 발행하며,
 * {@link OutboxAfterCommitRelay}가 트랜잭션 커밋 후 이 이벤트를 수신하여
 * 즉시 Redis Stream 발행을 시도한다.
 */
public record OutboxSavedEvent(
        Long outboxEventId,
        String streamKey,
        String payload
) {
}
