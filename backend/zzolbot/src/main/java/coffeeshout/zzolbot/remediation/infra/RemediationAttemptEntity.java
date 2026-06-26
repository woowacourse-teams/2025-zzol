package coffeeshout.zzolbot.remediation.infra;

import coffeeshout.zzolbot.remediation.domain.DefectType;
import coffeeshout.zzolbot.remediation.domain.RemediationStatus;
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
 * 한 모니터링 실행에서 시작한 자동 수정 시도의 이력. 앱이 워커로 작업을 넘긴 순간 DISPATCHED로 저장되고,
 * 워커의 내부 콜백이 PR_OPENED(+pr_url)·NO_FIX·FAILED로 갱신한다. 어드민 Monitor 탭이 알림 행에 인라인으로 보여준다.
 */
@Entity
@Table(
        name = "zzolbot_remediation_attempt",
        indexes = {
                @Index(name = "idx_zzolbot_remediation_attempt_run", columnList = "monitor_run_id, created_at DESC"),
                // 같은 fingerprint 재수정 쿨다운 가드(fingerprint=? AND created_at>?)용.
                @Index(name = "idx_zzolbot_remediation_attempt_cooldown", columnList = "fingerprint, created_at DESC")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RemediationAttemptEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "monitor_run_id", nullable = false)
    private Long monitorRunId;

    @Column(length = 200)
    private String fingerprint;

    @Enumerated(EnumType.STRING)
    @Column(name = "defect_type", nullable = false, length = 40)
    private DefectType defectType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RemediationStatus status;

    @Column(name = "pr_url", length = 500)
    private String prUrl;

    @Column(name = "pr_number")
    private Integer prNumber;

    @Column(name = "branch_name", length = 200)
    private String branchName;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static RemediationAttemptEntity dispatched(Long monitorRunId, String fingerprint, DefectType defectType) {
        final RemediationAttemptEntity entity = new RemediationAttemptEntity();
        entity.monitorRunId = monitorRunId;
        entity.fingerprint = fingerprint;
        entity.defectType = defectType;
        entity.status = RemediationStatus.DISPATCHED;
        final Instant now = Instant.now();
        entity.createdAt = now;
        entity.updatedAt = now;
        return entity;
    }

    public void markPrOpened(String prUrl, Integer prNumber, String branchName) {
        this.status = RemediationStatus.PR_OPENED;
        this.prUrl = prUrl;
        this.prNumber = prNumber;
        this.branchName = branchName;
        this.updatedAt = Instant.now();
    }

    public void markNoFix(String detail) {
        this.status = RemediationStatus.NO_FIX;
        this.detail = detail;
        this.updatedAt = Instant.now();
    }

    public void markFailed(String detail) {
        this.status = RemediationStatus.FAILED;
        this.detail = detail;
        this.updatedAt = Instant.now();
    }
}
