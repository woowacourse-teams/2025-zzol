package coffeeshout.zzolbot.monitor.application;

import coffeeshout.global.outbox.OutboxStatus;
import coffeeshout.global.redis.config.RedisStreamProperties;
import coffeeshout.zzolbot.infra.ZzolBotOutboxRepository;
import coffeeshout.zzolbot.monitor.config.MonitorProperties;
import coffeeshout.zzolbot.monitor.domain.MonitorSignal;
import coffeeshout.zzolbot.monitor.domain.MonitorSnapshot;
import coffeeshout.zzolbot.monitor.infra.LokiLogClient;
import coffeeshout.zzolbot.monitor.infra.PrometheusMetricClient;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 결정적(LLM 무관) 운영 신호를 수집한다. 평상시 매 주기 호출되므로 비용이 0이어야 한다.
 * 현재 신호: outbox DEAD_LETTER 누적 수, Redis Stream 최대 적재량(XLEN),
 * Loki ERROR 로그 건수, Loki WARN 로그 건수, HTTP 5xx 응답 수(Prometheus).
 * 로그(ERROR/WARN)와 5xx는 측정 축이 달라 — 로그는 모든 계층의 에러 이벤트, 5xx는 유저 영향 응답.
 * 겹치더라도 게이트가 OR라 알림은 1회이며, 서로의 사각지대를 메운다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonitorCollector {

    static final String SIGNAL_DEAD_LETTER = "outbox_dead_letter";
    static final String SIGNAL_STREAM_BACKLOG = "redis_stream_backlog";
    static final String SIGNAL_ERROR_LOGS = "loki_error_logs";
    static final String SIGNAL_WARN_LOGS = "loki_warn_logs";
    static final String SIGNAL_HTTP_5XX = "http_5xx";

    private final ZzolBotOutboxRepository outboxRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisStreamProperties redisStreamProperties;
    private final LokiLogClient lokiLogClient;
    private final PrometheusMetricClient prometheusMetricClient;
    private final MonitorProperties properties;
    private final Clock clock;

    public MonitorSnapshot collect() {
        final List<MonitorSignal> signals = new ArrayList<>();
        signals.add(collectDeadLetter());
        signals.add(collectStreamBacklog());
        signals.add(collectErrorLogs());
        signals.add(collectWarnLogs());
        signals.add(collectHttp5xx());
        return new MonitorSnapshot(signals, clock.instant());
    }

    private MonitorSignal collectErrorLogs() {
        final long count = lokiLogClient.countErrors(clock.instant(), properties.window());
        return MonitorSignal.of(SIGNAL_ERROR_LOGS, count, properties.errorLogThreshold());
    }

    private MonitorSignal collectWarnLogs() {
        final long count = lokiLogClient.countWarns(clock.instant(), properties.window());
        return MonitorSignal.of(SIGNAL_WARN_LOGS, count, properties.warnLogThreshold());
    }

    private MonitorSignal collectHttp5xx() {
        final long count = prometheusMetricClient.count5xx(clock.instant(), properties.window());
        return MonitorSignal.of(SIGNAL_HTTP_5XX, count, properties.http5xxThreshold());
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
