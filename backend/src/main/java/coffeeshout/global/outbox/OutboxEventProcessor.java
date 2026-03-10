package coffeeshout.global.outbox;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Outbox 이벤트의 DB 상태를 변경하는 트랜잭션 단위.
 * <p>
 * OutboxRelayWorker에서 직접 @Transactional을 쓰면
 * Redis I/O 동안 DB 커넥션을 물고 있게 되므로,
 * DB 조작만 따로 분리한 클래스다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventProcessor {

    private static final int MAX_RETRY_COUNT = 10;

    private final OutboxEventRepository outboxEventRepository;

    /**
     * PENDING 이벤트를 SKIP LOCKED로 조회하고 IN_PROGRESS로 전환 후 커밋한다.
     * 이 메서드가 반환되면 DB 커넥션은 즉시 반환된다.
     */
    @Transactional
    public List<OutboxEvent> fetchAndMarkInProgress(int batchSize) {
        final List<OutboxEvent> events = outboxEventRepository.findPendingEventsForUpdate(batchSize);
        events.forEach(OutboxEvent::markInProgress);
        return events;
    }

    /**
     * 발행 성공한 이벤트를 PUBLISHED로 전환한다.
     * <p>
     * REQUIRES_NEW를 사용하는 이유: AFTER_COMMIT 리스너에서 호출될 때
     * 기존 트랜잭션의 동기화 컨텍스트가 아직 활성 상태라서
     * REQUIRED로는 새 트랜잭션이 열리지 않고 변경이 반영되지 않는다.
     * Worker에서 호출할 때도 relay()가 @Transactional 없이 실행되므로
     * REQUIRES_NEW와 REQUIRED의 동작 차이가 없다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPublished(Long eventId) {
        outboxEventRepository.findById(eventId).ifPresent(OutboxEvent::markPublished);
    }

    /**
     * 발행 실패한 이벤트의 재시도 카운트를 증가시키고,
     * 최대 재시도 횟수를 초과하면 DEAD_LETTER로 전환한다.
     * 재시도 대상은 다시 PENDING으로 되돌린다.
     */
    @Transactional
    public void handleFailure(Long eventId) {
        outboxEventRepository.findById(eventId).ifPresent(this::processFailure);
    }

    private void processFailure(OutboxEvent event) {
        event.incrementRetryCount();

        if (event.getRetryCount() >= MAX_RETRY_COUNT) {
            event.markDeadLetter();
            log.error("Outbox 이벤트 최대 재시도 초과, DEAD_LETTER 전환: id={}, streamKey={}",
                    event.getId(), event.getStreamKey());
            return;
        }

        event.setStatusPending();
        log.warn("Outbox 이벤트 발행 실패, PENDING 복귀: id={}, retryCount={}",
                event.getId(), event.getRetryCount());
    }

    /**
     * IN_PROGRESS 상태에서 서버가 죽은 경우를 대비한 복구.
     * 5분 이상 IN_PROGRESS로 남아있는 이벤트를 PENDING으로 되돌린다.
     */
    @Transactional
    public int recoverStaleEvents() {
        final Instant threshold = Instant.now().minusSeconds(300);
        return outboxEventRepository.recoverStaleInProgressEvents(threshold, Instant.now());
    }
}
