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
                // firing 재배달 멱등 가드(dedup_key=? AND notified=true AND created_at>?)용 복합 인덱스.
                // fingerprint가 아니라 dedup_key로 잡는다 — 한 인시던트가 알림 2건으로 발화할 때
                // fingerprint는 서로 달라 가드를 통과해버리기 때문이다(#1598).
                @Index(name = "idx_zzolbot_monitor_run_dedup", columnList = "dedup_key, notified, created_at DESC")
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

    /**
     * 중복 분석 판정 키. 알림의 {@code incident_group} 라벨이 있으면 그것, 없으면 fingerprint다.
     * 같은 인시던트를 다른 각도에서 잡는 알림들이 이 키를 공유해 LLM 호출이 1회로 묶인다(#1598).
     */
    @Column(name = "dedup_key", length = 200)
    private String dedupKey;

    @Column(name = "analysis_summary", columnDefinition = "TEXT")
    private String analysisSummary;

    @Column(name = "suggested_actions_json", columnDefinition = "TEXT")
    private String suggestedActionsJson;

    @Column(nullable = false)
    private boolean notified;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public static MonitorRunEntity of(
            Instant now, Severity severity, String fingerprint, String dedupKey, String signalsJson) {
        final MonitorRunEntity entity = new MonitorRunEntity();
        entity.collectedAt = now;
        entity.anomalous = true;
        entity.severity = severity;
        entity.fingerprint = fingerprint;
        entity.dedupKey = dedupKey;
        entity.signalsJson = signalsJson;
        entity.notified = false;
        entity.createdAt = Instant.now();
        return entity;
    }

    /**
     * 같은 인시던트가 이미 분석돼 LLM 호출을 생략한 알림. 이력에는 남긴다 — 이전에는 중복이면
     * 저장 없이 early return해서 "무슨 알림이 왔었는지"가 아예 사라졌다(#1598).
     * <p>
     * {@code notified=false}로 둔다. 쿨다운 조회가 notified=true만 세므로, 중복 기록이 다시
     * 쿨다운의 기준점이 되어 창을 계속 연장하는 일을 막는다.
     */
    public static MonitorRunEntity duplicate(
            Instant now, Severity severity, String fingerprint, String dedupKey, String signalsJson, String reason) {
        final MonitorRunEntity entity = of(now, severity, fingerprint, dedupKey, signalsJson);
        entity.analysisSummary = reason;
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
