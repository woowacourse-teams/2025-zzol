package coffeeshout.zzolbot.monitor.application;

import coffeeshout.zzolbot.monitor.config.MonitorProperties;
import coffeeshout.zzolbot.monitor.domain.FiringAlert;
import coffeeshout.zzolbot.monitor.domain.MonitorAnalysis;
import coffeeshout.zzolbot.monitor.domain.Severity;
import coffeeshout.zzolbot.monitor.infra.AnomalyAnalyzer;
import coffeeshout.zzolbot.monitor.infra.LokiLogClient;
import coffeeshout.zzolbot.monitor.infra.MonitorRunEntity;
import coffeeshout.zzolbot.monitor.infra.MonitorRunRepository;
import coffeeshout.zzolbot.monitor.infra.ZzolBotSlackNotifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 능동 폴링 대신 Alertmanager가 발화한 firing 알림을 받아 LLM으로 보강한다(ADR-0032).
 * Alertmanager가 탐지·dedup을 소유하므로 자체 임계값 게이팅·쿨다운은 두지 않는다.
 * firing 알림을 영속하고, 예산이 있으면 ERROR 로그 샘플로 LLM 분석한 뒤 Slack에 게시한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertEnrichmentService implements FiringAlertEnricher {

    private static final int LOG_SAMPLE_LIMIT = 20;

    private final LlmCallBudget llmCallBudget;
    private final LokiLogClient lokiLogClient;
    private final AnomalyAnalyzer analyzer;
    private final ZzolBotSlackNotifier notifier;
    private final MonitorRunRepository monitorRunRepository;
    private final MonitorProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    public void enrich(FiringAlert alert) {
        if (!properties.enabled()) {
            log.debug("[ZzolBot] 모니터링 비활성 — 알림 보강 생략. fingerprint={}", alert.fingerprint());
            return;
        }
        final Instant now = clock.instant();
        final Severity severity = toSeverity(alert.severity());
        final MonitorRunEntity run = monitorRunRepository.save(
                MonitorRunEntity.of(now, severity, alert.fingerprint(), toJson(alertContext(alert))));

        final MonitorAnalysis analysis;
        if (llmCallBudget.tryAcquire()) {
            final List<String> logs = lokiLogClient.tailErrors(now, properties.window(), LOG_SAMPLE_LIMIT);
            analysis = safeAnalyze(alert, logs);
        } else {
            log.warn("[ZzolBot] 일일 LLM 예산 소진 — 이상 분석 생략. fingerprint={}", alert.fingerprint());
            analysis = MonitorAnalysis.budgetExhausted();
        }
        run.attachAnalysis(analysis.summary(), toJson(analysis.suggestedActions()));
        run.markNotified();
        notifier.notifyAnomaly(alert, analysis);
        monitorRunRepository.save(run);
    }

    private MonitorAnalysis safeAnalyze(FiringAlert alert, List<String> logSamples) {
        try {
            return analyzer.analyze(alert, logSamples);
        } catch (Exception e) {
            log.warn("[ZzolBot] 이상 분석 실패 — 결정적 알림만 전송. fingerprint={}", alert.fingerprint(), e);
            return MonitorAnalysis.failed();
        }
    }

    private Severity toSeverity(String severity) {
        if (severity == null || severity.isBlank()) {
            return Severity.WARNING;
        }
        return "critical".equalsIgnoreCase(severity.trim()) ? Severity.CRITICAL : Severity.WARNING;
    }

    private Map<String, Object> alertContext(FiringAlert alert) {
        final Map<String, Object> context = new LinkedHashMap<>();
        context.put("alertname", nullToEmpty(alert.alertname()));
        context.put("severity", nullToEmpty(alert.severity()));
        context.put("summary", nullToEmpty(alert.summary()));
        context.put("description", nullToEmpty(alert.description()));
        context.put("labels", alert.labels());
        return context;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("[ZzolBot] 모니터링 직렬화 실패", e);
            return value instanceof Map ? "{}" : "[]";
        }
    }
}
