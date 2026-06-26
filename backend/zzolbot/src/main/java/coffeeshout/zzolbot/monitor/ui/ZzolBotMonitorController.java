package coffeeshout.zzolbot.monitor.ui;

import coffeeshout.zzolbot.monitor.application.MonitorService;
import coffeeshout.zzolbot.monitor.infra.MonitorRunEntity;
import coffeeshout.zzolbot.remediation.infra.RemediationAttemptEntity;
import coffeeshout.zzolbot.remediation.infra.RemediationAttemptRepository;
import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 모니터링 어드민 API. Alertmanager가 분석·영속한 firing 알림 이력을 조회한다(조회 전용).
 * 각 알림에는 그 알림에서 시작한 최신 자동 수정 시도 상태(있으면)를 함께 실어, 어드민 Monitor 탭이
 * 한 화면에서 분석 결과와 수정 시도 상태/ PR 링크를 같이 보여준다.
 */
@RestController
@RequestMapping("/admin/zzolbot/monitor")
public class ZzolBotMonitorController {

    private final MonitorService monitorService;
    private final RemediationAttemptRepository attemptRepository;
    private final DateTimeFormatter formatter;

    public ZzolBotMonitorController(
            MonitorService monitorService, RemediationAttemptRepository attemptRepository, Clock clock) {
        this.monitorService = monitorService;
        this.attemptRepository = attemptRepository;
        this.formatter = DateTimeFormatter.ofPattern("MM/dd HH:mm").withZone(clock.getZone());
    }

    @GetMapping("/alerts")
    public List<AlertResponse> alerts() {
        final List<MonitorRunEntity> runs = monitorService.recentRuns();
        final Map<Long, RemediationAttemptEntity> latestByRun = latestAttempts(runs);
        return runs.stream()
                .map(run -> toResponse(run, latestByRun.get(run.getId())))
                .toList();
    }

    @GetMapping("/alerts/{id}")
    public ResponseEntity<AlertResponse> alert(@PathVariable Long id) {
        return monitorService.findRun(id)
                .map(run -> toResponse(run,
                        attemptRepository.findTopByMonitorRunIdOrderByCreatedAtDesc(run.getId()).orElse(null)))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 실행별 최신 수정 시도를 한 번의 쿼리로 모은다(목록 N+1 방지). created_at DESC 정렬이라 실행별 첫 항목이 최신이다.
     */
    private Map<Long, RemediationAttemptEntity> latestAttempts(List<MonitorRunEntity> runs) {
        final List<Long> ids = runs.stream().map(MonitorRunEntity::getId).toList();
        final Map<Long, RemediationAttemptEntity> latest = new HashMap<>();
        if (ids.isEmpty()) {
            return latest;
        }
        for (RemediationAttemptEntity attempt : attemptRepository.findByMonitorRunIdInOrderByCreatedAtDesc(ids)) {
            latest.putIfAbsent(attempt.getMonitorRunId(), attempt);
        }
        return latest;
    }

    private AlertResponse toResponse(MonitorRunEntity run, RemediationAttemptEntity attempt) {
        return new AlertResponse(
                run.getId(),
                run.isAnomalous(),
                run.getSeverity().name(),
                run.getSignalsJson(),
                run.getFingerprint(),
                run.getAnalysisSummary(),
                run.getRootCauseHypothesis(),
                run.getSuggestedActionsJson(),
                run.isNotified(),
                attempt == null ? null : attempt.getStatus().name(),
                attempt == null ? null : attempt.getPrUrl(),
                formatter.format(run.getCreatedAt()));
    }

    record AlertResponse(Long id, boolean anomalous, String severity, String signalsJson, String fingerprint,
                         String analysisSummary, String rootCauseHypothesis, String suggestedActionsJson,
                         boolean notified, String remediationStatus, String remediationPrUrl, String createdAt) {}
}
