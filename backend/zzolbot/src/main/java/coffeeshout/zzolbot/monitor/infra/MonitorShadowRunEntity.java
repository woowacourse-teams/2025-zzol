package coffeeshout.zzolbot.monitor.infra;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * 한 알림에 대해 권위 모델과 자체 모델이 각각 무엇이라 답했는지의 기록. <b>운영 판정에는 쓰이지
 * 않는다.</b> 두 시스템을 실제 알림에서 비교하기 위한 관측 전용 테이블이다.
 *
 * <p><b>운영 실행 이력과 테이블을 분리한 이유.</b> 섀도우 기록이 실패해도 운영 저장이 영향을 받으면
 * 안 된다. 같은 행에 합치면 섀도우 쪽 문제로 알림 이력 자체가 저장되지 않을 수 있다.
 *
 * <p>권위 쪽에는 인용 원문이 없다. 운영 코드가 인용 검증에 실패한 분석을 이미 강등하므로 권위의
 * {@code evidenceFound}는 접지 후 값이고, 섀도우는 접지 전과 후를 모두 남긴다. 두 값을 같은 뜻으로
 * 읽지 않도록 컬럼 이름을 나눴다.
 */
@Entity
@Table(
        name = "zzolbot_monitor_shadow_run",
        indexes = @Index(name = "idx_zzolbot_shadow_run_created_at", columnList = "created_at DESC"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MonitorShadowRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(length = 200)
    private String fingerprint;

    @Column(length = 200)
    private String alertname;

    @Column(name = "log_sample_count", nullable = false)
    private int logSampleCount;

    @Column(name = "authoritative_evidence_found", nullable = false)
    private boolean authoritativeEvidenceFound;

    @Column(name = "authoritative_summary", columnDefinition = "TEXT")
    private String authoritativeSummary;

    /** 인용 검증을 통과한 뒤의 판정. 운영 동작에 대응하는 값이다. */
    @Column(name = "shadow_evidence_found", nullable = false)
    private boolean shadowEvidenceFound;

    /** 모델이 스스로 주장한 판정. 인용 검증 전 값이라 모델의 판별력을 본다. */
    @Column(name = "shadow_claimed_evidence", nullable = false)
    private boolean shadowClaimedEvidence;

    @Column(name = "shadow_summary", columnDefinition = "TEXT")
    private String shadowSummary;

    @Column(name = "shadow_root_cause", columnDefinition = "TEXT")
    private String shadowRootCause;

    @Column(name = "shadow_evidence_line", columnDefinition = "TEXT")
    private String shadowEvidenceLine;

    @Column(name = "shadow_latency_ms", nullable = false)
    private long shadowLatencyMs;

    @Column(name = "shadow_failed", nullable = false)
    private boolean shadowFailed;

    @Column(name = "shadow_error", length = 500)
    private String shadowError;

    private MonitorShadowRunEntity(
            Instant createdAt,
            String fingerprint,
            String alertname,
            int logSampleCount,
            boolean authoritativeEvidenceFound,
            String authoritativeSummary,
            long shadowLatencyMs) {
        this.createdAt = createdAt;
        this.fingerprint = fingerprint;
        this.alertname = alertname;
        this.logSampleCount = logSampleCount;
        this.authoritativeEvidenceFound = authoritativeEvidenceFound;
        this.authoritativeSummary = authoritativeSummary;
        this.shadowLatencyMs = shadowLatencyMs;
    }

    public static MonitorShadowRunEntity compared(
            Instant createdAt,
            String fingerprint,
            String alertname,
            int logSampleCount,
            boolean authoritativeEvidenceFound,
            String authoritativeSummary,
            boolean shadowEvidenceFound,
            boolean shadowClaimedEvidence,
            String shadowSummary,
            String shadowRootCause,
            String shadowEvidenceLine,
            long shadowLatencyMs) {
        final MonitorShadowRunEntity entity = new MonitorShadowRunEntity(
                createdAt,
                fingerprint,
                alertname,
                logSampleCount,
                authoritativeEvidenceFound,
                authoritativeSummary,
                shadowLatencyMs);
        entity.shadowEvidenceFound = shadowEvidenceFound;
        entity.shadowClaimedEvidence = shadowClaimedEvidence;
        entity.shadowSummary = shadowSummary;
        entity.shadowRootCause = shadowRootCause;
        entity.shadowEvidenceLine = shadowEvidenceLine;
        return entity;
    }

    /** 자체 모델 호출이 실패한 경우. 실패도 기록해야 가용성을 셀 수 있다. */
    public static MonitorShadowRunEntity failed(
            Instant createdAt,
            String fingerprint,
            String alertname,
            int logSampleCount,
            boolean authoritativeEvidenceFound,
            String authoritativeSummary,
            long shadowLatencyMs,
            String error) {
        final MonitorShadowRunEntity entity = new MonitorShadowRunEntity(
                createdAt,
                fingerprint,
                alertname,
                logSampleCount,
                authoritativeEvidenceFound,
                authoritativeSummary,
                shadowLatencyMs);
        entity.shadowFailed = true;
        entity.shadowError = error;
        return entity;
    }
}
