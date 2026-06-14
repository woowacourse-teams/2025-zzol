package coffeeshout.global.outbox;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.redis.stream.StreamPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Outbox 테이블의 PENDING 이벤트를 Redis Stream으로 릴레이하는 Worker.
 * <p>
 * 핵심 설계: DB 트랜잭션과 Redis I/O를 철저히 분리한다.
 * <p>
 * 1단계: fetchAndMarkInProgress() — DB 트랜잭션으로 PENDING → IN_PROGRESS 전환 후 즉시 커밋 (DB 커넥션 반환)
 * 2단계: streamPublisher.publish() — 트랜잭션 밖에서 Redis I/O 수행 (타임아웃이 DB 커넥션에 영향 없음)
 * 3단계: markPublished() / handleFailure() — 단건 업데이트 트랜잭션
 * <p>
 * 이 분리가 없으면 Redis 장애 시 타임아웃 * 배치 크기만큼 DB 커넥션을 물고 있게 되어
 * HikariCP 풀이 고갈되는 Cascading Failure가 발생한다.
 */
@Slf4j
@Component
public class OutboxRelayWorker {

    private static final int BATCH_SIZE = 50;

    private final OutboxEventProcessor eventProcessor;
    private final OutboxEventRepository outboxEventRepository;
    private final StreamPublisher streamPublisher;
    private final ObjectMapper objectMapper;

    public OutboxRelayWorker(
            OutboxEventProcessor eventProcessor,
            OutboxEventRepository outboxEventRepository,
            StreamPublisher streamPublisher,
            @Qualifier("redisObjectMapper") ObjectMapper objectMapper
    ) {
        this.eventProcessor = eventProcessor;
        this.outboxEventRepository = outboxEventRepository;
        this.streamPublisher = streamPublisher;
        this.objectMapper = objectMapper;
    }

    /**
     * 500ms마다 Outbox 폴링.
     * <p>
     * 이 메서드에 @Transactional이 없는 것이 핵심이다.
     * Redis publish()가 3초 타임아웃 나더라도 DB 커넥션을 물고 있지 않는다.
     */
    @Scheduled(fixedDelayString = "${outbox.relay.delay:500}")
    public void relay() {
        // 1단계: DB 트랜잭션 — PENDING 조회 + IN_PROGRESS 전환 + 커밋 (DB 커넥션 즉시 반환)
        final List<OutboxEvent> events = eventProcessor.fetchAndMarkInProgress(BATCH_SIZE);

        if (events.isEmpty()) {
            return;
        }

        int successCount = 0;
        int failCount = 0;

        for (final OutboxEvent event : events) {
            try {
                // 2단계: 트랜잭션 밖에서 Redis I/O
                // 페이로드 무결성 검증 — 파싱 불가 페이로드는 발행하지 않고 실패 처리한다
                objectMapper.readValue(event.getPayload(), BaseEvent.class);
                streamPublisher.publish(event.getStreamKey(), event.getPayload(), event.getTraceparent());

                // 3단계: 단건 DB 트랜잭션 — PUBLISHED 전환
                eventProcessor.markPublished(event.getId());
                successCount++;
            } catch (Exception e) {
                // 3단계: 단건 DB 트랜잭션 — 실패 처리
                eventProcessor.handleFailure(event.getId());
                failCount++;
            }
        }

        if (successCount > 0 || failCount > 0) {
            log.info("Outbox relay 완료: success={}, fail={}", successCount, failCount);
        }
    }

    /**
     * IN_PROGRESS 상태에서 서버가 죽은 이벤트를 복구한다.
     * 1분마다 실행. 5분 이상 IN_PROGRESS로 남아있는 이벤트를 PENDING으로 되돌린다.
     */
    @Scheduled(fixedDelay = 60_000)
    public void recoverStaleEvents() {
        final int recovered = eventProcessor.recoverStaleEvents();
        if (recovered > 0) {
            log.warn("IN_PROGRESS 상태 복구: {}건 PENDING으로 전환", recovered);
        }
    }

    /**
     * 처리 완료된 Outbox 레코드를 정리한다.
     * 24시간 이전의 PUBLISHED 레코드를 삭제한다.
     */
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void cleanup() {
        final Instant threshold = Instant.now().minus(24, ChronoUnit.HOURS);
        final int deleted = outboxEventRepository.deletePublishedEventsBefore(threshold);
        log.info("Outbox 정리 완료: {}건 삭제", deleted);
    }
}
