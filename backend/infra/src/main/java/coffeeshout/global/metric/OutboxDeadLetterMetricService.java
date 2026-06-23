package coffeeshout.global.metric;

import coffeeshout.global.outbox.OutboxEventRepository;
import coffeeshout.global.outbox.OutboxStatus;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * outbox DEAD_LETTER 적체 깊이를 Prometheus 게이지로 노출한다(ADR-0032).
 *
 * <p>과거 zzol-bot 자체 폴링이 DB COUNT로 보던 DLQ 신호를 단일 알림 엔진(Prometheus 룰 →
 * Alertmanager)으로 옮기기 위한 메트릭이다. {@code RedisStreamLagMetricService}와 동일하게
 * Gauge는 Prometheus scrape 시점에만 평가되므로 상시 폴링이 없고, 조회는
 * {@code idx_outbox_status_id(status, id)} 인덱스를 타는 COUNT라 가볍다.
 *
 * <p>Prometheus 메트릭명: {@code outbox_dead_letter_count}
 */
@Slf4j
@Component
public class OutboxDeadLetterMetricService {

    private final OutboxEventRepository outboxEventRepository;
    private final MeterRegistry meterRegistry;

    public OutboxDeadLetterMetricService(
            OutboxEventRepository outboxEventRepository, MeterRegistry meterRegistry) {
        this.outboxEventRepository = outboxEventRepository;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void initializeMetrics() {
        Gauge.builder("outbox.dead_letter.count", this::deadLetterCount)
                .description("재시도 초과로 DEAD_LETTER 전환된 outbox 이벤트 누적 수")
                .register(meterRegistry);
        log.info("[Outbox] DEAD_LETTER 적체 게이지 등록 완료");
    }

    private double deadLetterCount() {
        try {
            return outboxEventRepository.countByStatus(OutboxStatus.DEAD_LETTER);
        } catch (Exception e) {
            log.warn("[Outbox] DEAD_LETTER 카운트 조회 실패 — NaN 처리", e);
            return Double.NaN;
        }
    }
}
