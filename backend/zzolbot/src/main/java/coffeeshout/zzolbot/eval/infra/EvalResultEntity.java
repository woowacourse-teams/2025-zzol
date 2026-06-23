package coffeeshout.zzolbot.eval.infra;

import coffeeshout.zzolbot.eval.domain.EvalVerdict;
import coffeeshout.zzolbot.eval.domain.JudgeScore;
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
 * 한 시나리오에 대한 평가 결과 한 건. judge 점수를 플랫 컬럼으로 보관한다.
 */
@Entity
@Table(
        name = "zzolbot_eval_result",
        indexes = {
                @Index(name = "idx_zzolbot_eval_result_run", columnList = "run_id"),
                @Index(name = "idx_zzolbot_eval_result_scenario", columnList = "scenario_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EvalResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", nullable = false)
    private Long runId;

    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Column(nullable = false)
    private int accuracy;

    @Column(nullable = false)
    private int groundedness;

    @Column(nullable = false)
    private boolean hallucination;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EvalVerdict verdict;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String rationale;

    @Column(name = "latency_ms", nullable = false)
    private long latencyMs;

    @Column(name = "missing_tool_calls", nullable = false)
    private int missingToolCalls;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public static EvalResultEntity create(
            Long runId, Long scenarioId, String answer, JudgeScore score, long latencyMs, int missingToolCalls) {
        final EvalResultEntity entity = new EvalResultEntity();
        entity.runId = runId;
        entity.scenarioId = scenarioId;
        entity.answer = answer;
        entity.accuracy = score.accuracy();
        entity.groundedness = score.groundedness();
        entity.hallucination = score.hallucinationDetected();
        entity.verdict = score.verdict();
        entity.rationale = score.rationale();
        entity.latencyMs = latencyMs;
        entity.missingToolCalls = missingToolCalls;
        entity.createdAt = Instant.now();
        return entity;
    }

    public boolean isPassed() {
        return verdict == EvalVerdict.PASS;
    }
}
