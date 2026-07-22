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
import java.util.regex.Pattern;
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
    private static final String JOB_LABEL = "job";
    private static final String JOB_SUFFIX = "-app";
    // 유도한 환경명이 LogQL 셀렉터에 삽입되므로 허용 문자를 제한한다 — 인젝션 방지 + 빈 문자열 차단.
    private static final Pattern SAFE_ENVIRONMENT = Pattern.compile("^[a-zA-Z0-9_-]+$");

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
        if (recentlyEnriched(alert.fingerprint(), now)) {
            log.info("[ZzolBot] 중복 분석 방지 — 일정 시간 내 동일 fingerprint 분석 이력 존재. fingerprint={}",
                    alert.fingerprint());
            return;
        }
        final Severity severity = toSeverity(alert.severity());
        final MonitorRunEntity run = monitorRunRepository.save(
                MonitorRunEntity.of(now, severity, alert.fingerprint(), toJson(alertContext(alert))));

        final String environment = resolveEnvironment(alert);
        final List<String> logs = lokiLogClient.tailErrors(now, properties.window(), LOG_SAMPLE_LIMIT, environment);
        final MonitorAnalysis analysis = analyze(alert, logs, environment);

        run.attachAnalysis(
                analysis.summary(),
                toJson(analysis.suggestedActions()),
                toJson(alertContext(alert, environment, logs.size(), analysis)));
        run.markNotified();
        notifier.notifyAnomaly(alert, analysis);
        monitorRunRepository.save(run);
    }

    /**
     * 알림의 {@code job} 라벨에서 조회할 환경을 유도한다({@code prod-app} → {@code prod}).
     * 라벨이 없거나 유도 결과가 환경명 형식에 맞지 않으면 이 인스턴스의 환경으로 폴백한다.
     * <p>
     * 폴백이 필요한 이유가 둘이다. (1) 룰이 {@code sum()}으로 라벨을 지우면 알림에 환경 정보가
     * 남지 않는다(#1592에서 실제로 발생). (2) 유도한 값은 하류 {@link LokiLogClient#tailErrors}의
     * LogQL 셀렉터에 그대로 삽입되므로, 형식을 검증하지 않으면 셀렉터 인젝션 위험이 있고
     * {@code job}이 정확히 {@code "-app"}일 때 빈 문자열이 되어 환경 필터가 무력화된다(#1595 리뷰).
     */
    private String resolveEnvironment(FiringAlert alert) {
        final String job = alert.labels().get(JOB_LABEL);
        if (job == null || job.isBlank()) {
            return fallbackEnvironment(alert, "job 라벨 없음");
        }
        final String trimmed = job.trim();
        final String derived = trimmed.endsWith(JOB_SUFFIX)
                ? trimmed.substring(0, trimmed.length() - JOB_SUFFIX.length())
                : trimmed;
        if (!SAFE_ENVIRONMENT.matcher(derived).matches()) {
            return fallbackEnvironment(alert, "환경명 형식 위반(job=%s)".formatted(job));
        }
        return derived;
    }

    private String fallbackEnvironment(FiringAlert alert, String reason) {
        final String fallback = lokiLogClient.defaultEnvironment();
        log.info("[ZzolBot] {} — 실행 환경({})으로 폴백. fingerprint={}",
                reason, fallback, alert.fingerprint());
        return fallback;
    }

    /**
     * 로그 근거를 먼저 확보하고, 근거가 있을 때만 예산을 소모해 LLM을 호출한다.
     * <p>
     * 근거 없이 LLM을 부르면 알림 메타데이터만으로 그럴듯한 인과를 지어낸다(#1594). 예산 확인을
     * 로그 조회 뒤로 옮긴 이유이기도 하다 — 어차피 분석하지 않을 건에 예산 슬롯을 태우지 않는다.
     */
    private MonitorAnalysis analyze(FiringAlert alert, List<String> logs, String environment) {
        if (logs.isEmpty()) {
            log.info("[ZzolBot] 조회 구간에 ERROR 로그 없음 — LLM 분석 생략. environment={} fingerprint={}",
                    environment, alert.fingerprint());
            return MonitorAnalysis.noEvidence(environment, (int) properties.window().toMinutes());
        }
        if (!llmCallBudget.tryAcquire()) {
            log.warn("[ZzolBot] 일일 LLM 예산 소진 — 이상 분석 생략. fingerprint={}", alert.fingerprint());
            return MonitorAnalysis.budgetExhausted();
        }
        return safeAnalyze(alert, logs, environment);
    }

    /**
     * 같은 fingerprint를 일정 시간 안에는 다시 분석하지 않는다(지문별 중복 분석 방지). 웹훅 재시도·flapping을
     * 흡수하고, 지속되는 장애를 매 재통보마다 다시 LLM에 태우지 않아 비용을 묶는다. 간격이 0이거나
     * fingerprint가 비어 식별 불가하면 가드하지 않는다. Alertmanager {@code repeat_interval}(4h) 위의
     * 앱측 방어선으로, 재분석을 fingerprint당 일정 시간에 한 번으로 제한한다.
     */
    private boolean recentlyEnriched(String fingerprint, Instant now) {
        final Duration cooldown = properties.enrichCooldown();
        if (cooldown.isZero() || fingerprint == null || fingerprint.isBlank()) {
            return false;
        }
        return monitorRunRepository.existsByFingerprintAndNotifiedTrueAndCreatedAtAfter(
                fingerprint, now.minus(cooldown));
    }

    private MonitorAnalysis safeAnalyze(FiringAlert alert, List<String> logSamples, String environment) {
        try {
            return analyzer.analyze(alert, logSamples, environment);
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

    /**
     * 알림 컨텍스트에 "무엇을 근거로 분석했는가"를 덧붙인다. 분석이 틀렸을 때 어드민 화면만 보고
     * 검증할 수 있게 하려는 것으로, 진단 챗 경로가 답변에 조회 기준을 남기는 것과 같은 취지다(#1594).
     * <p>
     * 전용 컬럼 대신 기존 {@code signals_json}에 병합해 마이그레이션 없이 처리한다. 입력(알림)과
     * 분석 메타데이터가 한 컬럼에 섞이는 절충이며, 별도 키로 분리해 구분한다.
     */
    private Map<String, Object> alertContext(
            FiringAlert alert, String environment, int logSampleCount, MonitorAnalysis analysis) {
        final Map<String, Object> context = alertContext(alert);
        final Map<String, Object> analysisContext = new LinkedHashMap<>();
        analysisContext.put("logEnvironment", environment);
        analysisContext.put("windowMinutes", properties.window().toMinutes());
        analysisContext.put("logSampleCount", logSampleCount);
        analysisContext.put("evidenceFound", analysis.evidenceFound());
        analysisContext.put("rootCauseHypothesis", nullToEmpty(analysis.rootCauseHypothesis()));
        context.put("analysisContext", analysisContext);
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
