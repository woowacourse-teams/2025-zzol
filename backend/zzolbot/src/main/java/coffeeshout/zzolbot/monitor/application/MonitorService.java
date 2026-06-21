package coffeeshout.zzolbot.monitor.application;

import coffeeshout.zzolbot.monitor.config.MonitorProperties;
import coffeeshout.zzolbot.monitor.domain.AnomalyVerdict;
import coffeeshout.zzolbot.monitor.domain.MonitorAnalysis;
import coffeeshout.zzolbot.monitor.domain.MonitorSnapshot;
import coffeeshout.zzolbot.monitor.infra.AnomalyAnalyzer;
import coffeeshout.zzolbot.monitor.infra.LokiLogClient;
import coffeeshout.zzolbot.monitor.infra.MonitorRunEntity;
import coffeeshout.zzolbot.monitor.infra.MonitorRunRepository;
import coffeeshout.zzolbot.monitor.infra.ZzolBotSlackNotifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 능동 모니터링 한 주기를 수행한다: 결정적 수집 → 임계값 게이팅 → (이상·비쿨다운·예산 보유 시) LLM 분석·알림 → 저장.
 * 스케줄러와 어드민 수동 트리거가 공유한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitorService {

    private static final int LOG_SAMPLE_LIMIT = 20;

    private final MonitorCollector collector;
    private final AnomalyGate gate;
    private final AnomalyAnalyzer analyzer;
    private final LokiLogClient lokiLogClient;
    private final ZzolBotSlackNotifier notifier;
    private final MonitorRunRepository monitorRunRepository;
    private final LlmCallBudget llmCallBudget;
    private final MonitorProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public MonitorRunEntity runOnce() {
        final MonitorSnapshot snapshot = collector.collect();
        final AnomalyVerdict verdict = gate.evaluate(snapshot);
        final MonitorRunEntity run = MonitorRunEntity.of(snapshot.collectedAt(), verdict, toJson(snapshot));

        if (verdict.anomalous() && !inCooldown(verdict)) {
            analyzeAndNotify(snapshot, verdict, run);
            run.markNotified();
        }
        return monitorRunRepository.save(run);
    }

    public List<MonitorRunEntity> recentRuns() {
        return monitorRunRepository.findTop50ByOrderByCreatedAtDesc();
    }

    public Optional<MonitorRunEntity> findRun(Long id) {
        return monitorRunRepository.findById(id);
    }

    private void analyzeAndNotify(MonitorSnapshot snapshot, AnomalyVerdict verdict, MonitorRunEntity run) {
        final MonitorAnalysis analysis;
        if (llmCallBudget.tryAcquire()) {
            final List<String> logSamples = lokiLogClient.tailErrors(
                    snapshot.collectedAt(), properties.window(), LOG_SAMPLE_LIMIT);
            analysis = safeAnalyze(snapshot, verdict, logSamples);
        } else {
            log.warn("[ZzolBot] 일일 LLM 예산 소진 — 이상 분석 생략. fingerprint={}", verdict.fingerprint());
            analysis = MonitorAnalysis.budgetExhausted();
        }
        run.attachAnalysis(analysis.summary(), toJson(analysis.suggestedActions()));
        notifier.notifyAnomaly(snapshot, verdict, analysis);
    }

    private MonitorAnalysis safeAnalyze(MonitorSnapshot snapshot, AnomalyVerdict verdict, List<String> logSamples) {
        try {
            return analyzer.analyze(snapshot, verdict, logSamples);
        } catch (Exception e) {
            log.warn("[ZzolBot] 이상 분석 실패 — 결정적 신호만 알림. fingerprint={}", verdict.fingerprint(), e);
            return MonitorAnalysis.failed();
        }
    }

    private boolean inCooldown(AnomalyVerdict verdict) {
        // 같은 fingerprint의 직전 알림만 조회해야, 그 사이 다른 이상이 끼어도 동일 이상이 쿨다운으로 정확히 억제된다.
        return monitorRunRepository
                .findFirstByFingerprintAndNotifiedTrueOrderByCreatedAtDesc(verdict.fingerprint())
                .filter(last -> last.getCreatedAt()
                        .isAfter(clock.instant().minus(Duration.ofMinutes(properties.cooldownMinutes()))))
                .isPresent();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("[ZzolBot] 모니터링 직렬화 실패", e);
            return "[]";
        }
    }
}
