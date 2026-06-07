package coffeeshout.global.outbox;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.redis.stream.StreamKey;
import coffeeshout.global.redis.stream.StreamTracePropagator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이벤트를 Outbox 테이블에 저장하는 단일 진입점.
 * <p>
 * 저장 후 {@link OutboxSavedEvent}를 Spring 내부 이벤트로 발행하여,
 * 트랜잭션 커밋 직후 {@link OutboxAfterCommitRelay}가 즉시 Redis 발행을 시도하게 한다.
 * <p>
 * 이것이 2단 콤보 Outbox의 핵심이다:
 * - 평상시: 커밋 즉시 Redis 발행 (0ms 지연)
 * - Redis 장애 시: Outbox에 PENDING으로 남아있어 Worker가 500ms 후 재시도
 */
@Slf4j
@Component
public class OutboxEventRecorder {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final StreamTracePropagator streamTracePropagator;

    public OutboxEventRecorder(
            OutboxEventRepository outboxEventRepository,
            @Qualifier("redisObjectMapper") ObjectMapper objectMapper,
            ApplicationEventPublisher applicationEventPublisher,
            StreamTracePropagator streamTracePropagator
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.applicationEventPublisher = applicationEventPublisher;
        this.streamTracePropagator = streamTracePropagator;
    }

    /**
     * 이벤트를 Outbox 테이블에 저장하고, 트랜잭션 커밋 직후 즉시 발행을 트리거한다.
     * <p>
     * 호출자에 트랜잭션이 있으면 해당 트랜잭션에 참여하여
     * 비즈니스 데이터와 Outbox 레코드가 함께 커밋된다.
     * 호출자에 트랜잭션이 없으면 자체 트랜잭션을 생성한다.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void record(StreamKey streamKey, BaseEvent event) {
        try {
            final String payload = objectMapper.writeValueAsString(event);
            // 재시도 릴레이는 스케줄러 스레드라 컨텍스트가 없으므로 기록 시점에 traceparent를 캡처한다
            final String traceparent = streamTracePropagator.currentTraceparent();
            final OutboxEvent outboxEvent = OutboxEvent.create(streamKey.getRedisKey(), payload, null, traceparent);
            outboxEventRepository.saveAndFlush(outboxEvent);

            // Spring 내부 이벤트 발행 → 트랜잭션 커밋 후 OutboxAfterCommitRelay가 수신
            applicationEventPublisher.publishEvent(
                    new OutboxSavedEvent(outboxEvent.getId(), streamKey.getRedisKey(), payload, traceparent)
            );

            log.debug("Outbox 이벤트 저장: streamKey={}, eventId={}",
                    streamKey.getRedisKey(), event.eventId());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("이벤트 직렬화 실패: " + e.getMessage(), e);
        }
    }
}
