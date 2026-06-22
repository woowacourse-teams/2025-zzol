package coffeeshout.zzolbot.eval.infra;

import coffeeshout.zzolbot.eval.domain.EvalRunStatus;
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
 * 평가 일괄 실행 단위. label/model/promptVersion으로 A/B 비교의 한 축을 식별한다.
 */
@Entity
@Table(
        name = "zzolbot_eval_run",
        indexes = @Index(name = "idx_zzolbot_eval_run_started_at", columnList = "started_at DESC")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EvalRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String label;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(name = "prompt_version", length = 100)
    private String promptVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EvalRunStatus status;

    @Column(name = "scenario_count", nullable = false)
    private int scenarioCount;

    @Column(name = "pass_count", nullable = false)
    private int passCount;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    public static EvalRunEntity start(String label, String model, String promptVersion, int scenarioCount) {
        final EvalRunEntity entity = new EvalRunEntity();
        entity.label = label;
        entity.model = model;
        entity.promptVersion = promptVersion;
        entity.scenarioCount = scenarioCount;
        entity.status = EvalRunStatus.RUNNING;
        entity.passCount = 0;
        entity.startedAt = Instant.now();
        return entity;
    }

    public void complete(int passCount) {
        this.passCount = passCount;
        this.status = EvalRunStatus.COMPLETED;
        this.finishedAt = Instant.now();
    }

    public void fail() {
        this.status = EvalRunStatus.FAILED;
        this.finishedAt = Instant.now();
    }
}
