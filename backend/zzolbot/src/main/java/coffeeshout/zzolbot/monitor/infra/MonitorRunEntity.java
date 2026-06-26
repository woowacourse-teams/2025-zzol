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
                // firing 재배달 멱등 가드(fingerprint=? AND notified=true AND created_at>?)용 복합 인덱스.
                // 단일 fingerprint 인덱스는 이 인덱스의 prefix라 중복이므로 두지 않는다.
                @Index(name = "idx_zzolbot_monitor_run_cooldown", columnList = "fingerprint, notified, created_at DESC")
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

    // LLM이 추정한 근본원인 가설. 자동 수정 봇의 결함 분류·결함 위치 특정 입력으로 쓰인다(zzolbot remediation).
    @Column(name = "root_cause_hypothesis", columnDefinition = "TEXT")
    private String rootCauseHypothesis;

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

    public void attachAnalysis(String analysisSummary, String rootCauseHypothesis, String suggestedActionsJson) {
        this.analysisSummary = analysisSummary;
        this.rootCauseHypothesis = rootCauseHypothesis;
        this.suggestedActionsJson = suggestedActionsJson;
    }

    public void markNotified() {
        this.notified = true;
    }
}
