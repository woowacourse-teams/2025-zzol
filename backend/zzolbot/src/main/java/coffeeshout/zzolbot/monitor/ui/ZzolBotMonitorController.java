package coffeeshout.zzolbot.monitor.ui;

import coffeeshout.zzolbot.monitor.application.MonitorService;
import coffeeshout.zzolbot.monitor.infra.MonitorRunEntity;
import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 모니터링 어드민 API. Alertmanager가 분석·영속한 firing 알림 이력을 조회한다(조회 전용).
 */
@RestController
@RequestMapping("/admin/zzolbot/monitor")
public class ZzolBotMonitorController {

    private final MonitorService monitorService;
    private final DateTimeFormatter formatter;

    public ZzolBotMonitorController(MonitorService monitorService, Clock clock) {
        this.monitorService = monitorService;
        this.formatter = DateTimeFormatter.ofPattern("MM/dd HH:mm").withZone(clock.getZone());
    }

    @GetMapping("/alerts")
    public List<AlertResponse> alerts() {
        return monitorService.recentRuns().stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/alerts/{id}")
    public ResponseEntity<AlertResponse> alert(@PathVariable Long id) {
        return monitorService.findRun(id)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private AlertResponse toResponse(MonitorRunEntity run) {
        return new AlertResponse(
                run.getId(),
                run.isAnomalous(),
                run.getSeverity().name(),
                run.getSignalsJson(),
                run.getFingerprint(),
                run.getAnalysisSummary(),
                run.getSuggestedActionsJson(),
                run.isNotified(),
                formatter.format(run.getCreatedAt()));
    }

    record AlertResponse(Long id, boolean anomalous, String severity, String signalsJson, String fingerprint,
                         String analysisSummary, String suggestedActionsJson, boolean notified, String createdAt) {}
}
