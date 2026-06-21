package coffeeshout.zzolbot.monitor.application;

import coffeeshout.global.outbox.OutboxStatus;
import coffeeshout.global.redis.config.RedisStreamProperties;
import coffeeshout.zzolbot.infra.ZzolBotOutboxRepository;
import coffeeshout.zzolbot.monitor.config.MonitorProperties;
import coffeeshout.zzolbot.monitor.domain.MonitorSignal;
import coffeeshout.zzolbot.monitor.domain.MonitorSnapshot;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 결정적(LLM 무관) 운영 신호를 수집한다. 평상시 매 주기 호출되므로 비용이 0이어야 한다.
 * 현재 신호: outbox DEAD_LETTER 누적 수, Redis Stream 최대 적재량(XLEN).
 * (Loki ERROR/WARN·Prometheus 등 HTTP 기반 신호는 후속 확장 여지로 남김.)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonitorCollector {

    static final String SIGNAL_DEAD_LETTER = "outbox_dead_letter";
    static final String SIGNAL_STREAM_BACKLOG = "redis_stream_backlog";

    private final ZzolBotOutboxRepository outboxRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisStreamProperties redisStreamProperties;
    private final MonitorProperties properties;
    private final Clock clock;

    public MonitorSnapshot collect() {
        final List<MonitorSignal> signals = new ArrayList<>();
        signals.add(collectDeadLetter());
        signals.add(collectStreamBacklog());
        return new MonitorSnapshot(signals, clock.instant());
    }

    private MonitorSignal collectDeadLetter() {
        final long count = outboxRepository.countByStatusIn(List.of(OutboxStatus.DEAD_LETTER));
        return MonitorSignal.of(SIGNAL_DEAD_LETTER, count, properties.deadLetterThreshold());
    }

    private MonitorSignal collectStreamBacklog() {
        long maxBacklog = 0;
        for (String streamKey : redisStreamProperties.keys().keySet()) {
            final Long size = redisTemplate.opsForStream().size(streamKey);
            if (size != null) {
                maxBacklog = Math.max(maxBacklog, size);
            }
        }
        return MonitorSignal.of(SIGNAL_STREAM_BACKLOG, maxBacklog, properties.streamBacklogThreshold());
    }
}
