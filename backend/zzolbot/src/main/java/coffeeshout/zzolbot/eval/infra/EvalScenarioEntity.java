package coffeeshout.zzolbot.eval.infra;

import coffeeshout.zzolbot.eval.domain.ScenarioKind;
import coffeeshout.zzolbot.eval.domain.ScenarioSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 평가 골든 시나리오. 질문 + 박제된 입력(JSON) + 채점 기준(rubric)을 보관한다.
 * 박제 입력의 해석은 kind에 따라 앱 계층이 담당한다 — CHAT은 {@code ToolSnapshot}(도구 결과),
 * MONITOR는 알림 분석 픽스처. 엔티티는 JSON 문자열만 보관한다.
 */
@Entity
@Table(
        name = "zzolbot_eval_scenario",
        uniqueConstraints = @UniqueConstraint(name = "uk_zzolbot_eval_scenario_name", columnNames = "name"),
        indexes = @Index(name = "idx_zzolbot_eval_scenario_created_at", columnList = "created_at DESC")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EvalScenarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScenarioKind kind;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "snapshot_json", nullable = false, columnDefinition = "LONGTEXT")
    private String snapshotJson;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String rubric;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private ScenarioSource sourceType;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public static EvalScenarioEntity create(
            String name, ScenarioKind kind, String question, String snapshotJson, String rubric,
            ScenarioSource sourceType) {
        final EvalScenarioEntity entity = new EvalScenarioEntity();
        entity.name = name;
        entity.kind = kind;
        entity.question = question;
        entity.snapshotJson = snapshotJson;
        entity.rubric = rubric;
        entity.sourceType = sourceType;
        entity.createdAt = Instant.now();
        return entity;
    }
}
