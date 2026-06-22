package coffeeshout.zzolbot.monitor.infra;

import coffeeshout.zzolbot.monitor.domain.Severity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 한 모니터링 주기의 실행 이력. 결정적 신호(JSON)와 판정을 항상 저장하고,
 * 이상으로 LLM 분석이 수행된 경우 요약·조치 제안을 덧붙인다.
 */
@Entity
@Table(
        name = "zzolbot_monitor_run",
        indexes = {
                @Index(name = "idx_zzolbot_monitor_run_created_at", columnList = "created_at DESC"),
                @Index(name = "idx_zzolbot_monitor_run_fingerprint", columnList = "fingerprint")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MonitorRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "collected_at", nullable = false)
    private Instant collectedAt;

    @Column(nullable = false)
    private boolean anomalous;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    @Column(name = "signals_json", nullable = false, columnDefinition = "TEXT")
    private String signalsJson;

    @Column(length = 200)
    private String fingerprint;

    @Column(name = "analysis_summary", columnDefinition = "TEXT")
    private String analysisSummary;

    @Column(name = "suggested_actions_json", columnDefinition = "TEXT")
    private String suggestedActionsJson;

    @Column(nullable = false)
    private boolean notified;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public static MonitorRunEntity of(Instant now, Severity severity, String fingerprint, String signalsJson) {
        final MonitorRunEntity entity = new MonitorRunEntity();
        entity.collectedAt = now;
        entity.anomalous = true;
        entity.severity = severity;
        entity.fingerprint = fingerprint;
        entity.signalsJson = signalsJson;
        entity.notified = false;
        entity.createdAt = Instant.now();
        return entity;
    }

    public void attachAnalysis(String analysisSummary, String suggestedActionsJson) {
        this.analysisSummary = analysisSummary;
        this.suggestedActionsJson = suggestedActionsJson;
    }

    public void markNotified() {
        this.notified = true;
    }
}
