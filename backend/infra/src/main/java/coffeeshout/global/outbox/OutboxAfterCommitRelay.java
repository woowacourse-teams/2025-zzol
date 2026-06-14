package coffeeshout.global.outbox;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.redis.stream.StreamPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 트랜잭션 커밋 직후 Outbox 이벤트를 즉시 Redis Stream에 발행하는 릴레이.
 * <p>
 * 2단 콤보 Outbox의 1단계(즉시 발행)를 담당한다.
 * <p>
 * 동작 흐름:
 * 1. {@link OutboxEventRecorder}가 Outbox 저장 + {@link OutboxSavedEvent} 발행
 * 2. 트랜잭션이 커밋되면 이 리스너가 실행되어 Redis에 즉시 발행 시도
 * 3. 성공 시 → Outbox 레코드를 PUBLISHED로 전환 (REQUIRES_NEW 트랜잭션)
 * 4. 실패 시 → 예외를 삼킴. Outbox에 PENDING으로 남아있으므로
 *    {@link OutboxRelayWorker}가 500ms 후 재시도
 */
@Slf4j
@Component
public class OutboxAfterCommitRelay {

    private final StreamPublisher streamPublisher;
    private final OutboxEventProcessor eventProcessor;
    private final ObjectMapper objectMapper;

    public OutboxAfterCommitRelay(
            StreamPublisher streamPublisher,
            OutboxEventProcessor eventProcessor,
            @Qualifier("redisObjectMapper") ObjectMapper objectMapper
    ) {
        this.streamPublisher = streamPublisher;
        this.eventProcessor = eventProcessor;
        this.objectMapper = objectMapper;
    }

    /**
     * 트랜잭션 커밋 직후 실행된다.
     * <p>
     * AFTER_COMMIT이므로 이 시점에서 DB에는 비즈니스 데이터 + Outbox 레코드가
     * 모두 커밋된 상태다. Redis 발행이 실패해도 DB 데이터에는 영향이 없다.
     * <p>
     * markPublished()는 REQUIRES_NEW로 새 트랜잭션을 강제한다.
     * AFTER_COMMIT 리스너 내에서는 원래 트랜잭션의 동기화 컨텍스트가 아직 활성 상태라서,
     * REQUIRED로 호출하면 이미 커밋된 트랜잭션에 참여하려 해서 변경이 반영되지 않기 때문이다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOutboxSaved(OutboxSavedEvent savedEvent) {
        try {
            // 페이로드 무결성 검증 — 파싱 불가 페이로드는 발행 전에 실패시켜 Worker 재시도 경로로 보낸다
            objectMapper.readValue(savedEvent.payload(), BaseEvent.class);
            streamPublisher.publish(savedEvent.streamKey(), savedEvent.payload(), savedEvent.traceparent());

            // 즉시 발행 성공 → PUBLISHED로 전환 (REQUIRES_NEW 트랜잭션)
            eventProcessor.markPublished(savedEvent.outboxEventId());

            log.debug("Outbox 즉시 발행 성공: outboxId={}, streamKey={}",
                    savedEvent.outboxEventId(), savedEvent.streamKey());
        } catch (Exception e) {
            // 즉시 발행 실패 → 무시. Outbox에 PENDING으로 남아있으므로 Worker가 재시도
            log.warn("Outbox 즉시 발행 실패 (Worker가 재시도 예정): outboxId={}, streamKey={}, error={}",
                    savedEvent.outboxEventId(), savedEvent.streamKey(), e.getMessage());
        }
    }
}
