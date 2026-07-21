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
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 능동 폴링 대신 Alertmanager가 발화한 firing 알림을 받아 LLM으로 분석한다(ADR-0032).
 * 탐지는 Alertmanager가 소유하고, 앱은 지문별 재분석 간격으로 같은 장애의 LLM 재호출만 비용 관점에서 묶는다.
 * firing 알림을 영속하고, 예산이 있으면 ERROR 로그 샘플로 LLM 분석한 뒤 Slack에 게시한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertEnrichmentService implements FiringAlertEnricher {

    private static final int LOG_SAMPLE_LIMIT = 20;
    private static final String INCIDENT_GROUP_LABEL = "incident_group";

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
            log.debug("[ZzolBot] 모니터링 비활성 — 알림 분석 생략. fingerprint={}", alert.fingerprint());
            return;
        }
        final Instant now = clock.instant();
        final Severity severity = toSeverity(alert.severity());
        final String dedupKey = resolveDedupKey(alert);
        if (recentlyEnriched(dedupKey, now)) {
            log.info("[ZzolBot] 중복 분석 방지 — 일정 시간 내 같은 인시던트 분석 이력 존재. dedupKey={} fingerprint={}",
                    dedupKey, alert.fingerprint());
            monitorRunRepository.save(MonitorRunEntity.duplicate(
                    now, severity, alert.fingerprint(), dedupKey, toJson(alertContext(alert)),
                    "LLM 분석 생략 — 같은 인시던트(%s)가 이미 분석됨".formatted(dedupKey)));
            return;
        }
        final MonitorRunEntity run = monitorRunRepository.save(
                MonitorRunEntity.of(now, severity, alert.fingerprint(), dedupKey, toJson(alertContext(alert))));

        final MonitorAnalysis analysis = analyze(alert, now);
        run.attachAnalysis(analysis.summary(), toJson(analysis.suggestedActions()));
        run.markNotified();
        notifier.notifyAnomaly(alert, analysis);
        monitorRunRepository.save(run);
    }

    private MonitorAnalysis analyze(FiringAlert alert, Instant now) {
        if (!llmCallBudget.tryAcquire()) {
            log.warn("[ZzolBot] 일일 LLM 예산 소진 — 이상 분석 생략. fingerprint={}", alert.fingerprint());
            return MonitorAnalysis.budgetExhausted();
        }
        final List<String> logs = lokiLogClient.tailErrors(now, properties.window(), LOG_SAMPLE_LIMIT);
        return safeAnalyze(alert, logs);
    }

    /**
     * 중복 분석 판정 키를 정한다. 알림에 {@code incident_group} 라벨이 있으면 그것을, 없으면
     * fingerprint를 쓴다.
     * <p>
     * 하나의 인시던트가 알림 2건으로 발화하는 경우가 있다 — IP 차단 급증은 신규 BAN(warning)과
     * 차단 요청(critical) 두 룰이 같은 현상을 다른 각도에서 잡는다. fingerprint는 알림마다 다르므로
     * 그것으로 가드하면 같은 장애에 LLM을 두 번 태운다. 관련 룰에 공통 라벨을 달아 한 번으로 묶는다.
     * <p>
     * 억제(inhibit)로 warning을 죽이지 않는 이유는 그것이 선행 신호이기 때문이다. warning만 뜨고
     * critical로 번지지 않은 사례가 실제로 있어(2026-07-13), 죽이면 조기 경보를 잃는다.
     */
    private String resolveDedupKey(FiringAlert alert) {
        final String incidentGroup = alert.labels().get(INCIDENT_GROUP_LABEL);
        if (incidentGroup == null || incidentGroup.isBlank()) {
            return alert.fingerprint();
        }
        return incidentGroup.trim();
    }

    /**
     * 같은 인시던트를 일정 시간 안에는 다시 분석하지 않는다. 웹훅 재시도·flapping을 흡수하고,
     * 지속되는 장애를 매 재통보마다 다시 LLM에 태우지 않아 비용을 묶는다. 간격이 0이거나
     * 키가 비어 식별 불가하면 가드하지 않는다. Alertmanager {@code repeat_interval}(4h) 위의
     * 앱측 방어선이다.
     */
    private boolean recentlyEnriched(String dedupKey, Instant now) {
        final Duration cooldown = properties.enrichCooldown();
        if (cooldown.isZero() || dedupKey == null || dedupKey.isBlank()) {
            return false;
        }
        return monitorRunRepository.existsByDedupKeyAndNotifiedTrueAndCreatedAtAfter(
                dedupKey, now.minus(cooldown));
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
