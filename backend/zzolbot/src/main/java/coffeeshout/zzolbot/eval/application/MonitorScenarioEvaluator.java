package coffeeshout.zzolbot.eval.application;

import coffeeshout.zzolbot.eval.domain.MonitorScenarioFixture;
import coffeeshout.zzolbot.eval.domain.ScenarioKind;
import coffeeshout.zzolbot.eval.infra.EvalScenarioEntity;
import coffeeshout.zzolbot.eval.infra.MonitorFixtureCodec;
import coffeeshout.zzolbot.monitor.domain.MonitorAnalysis;
import coffeeshout.zzolbot.monitor.infra.AnomalyAnalyzer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 알림 분석(무인) 경로 평가기. 박제된 알림·로그 샘플을 {@link AnomalyAnalyzer}에 직접 주입해
 * LLM 추론만 라이브로 실행한다. {@code AlertEnrichmentService}를 거치지 않으므로
 * 일일 예산 소모·Slack 발송·monitor_run 영속이 발생하지 않는다.
 * 채점 대상은 근거 인용 검증(강등 포함)을 거친 최종 분석 — 실제 Slack에 나가는 그 형태다.
 */
@Component
@RequiredArgsConstructor
public class MonitorScenarioEvaluator implements ScenarioEvaluator {

    private final AnomalyAnalyzer analyzer;
    private final MonitorFixtureCodec codec;

    @Override
    public ScenarioKind kind() {
        return ScenarioKind.MONITOR;
    }

    @Override
    public EvalAnswer evaluate(EvalScenarioEntity scenario) {
        final MonitorScenarioFixture fixture = codec.fromJson(scenario.getSnapshotJson());
        final MonitorAnalysis analysis =
                analyzer.analyze(fixture.alert(), fixture.logSamples(), fixture.logEnvironment());
        return new EvalAnswer(flatten(analysis), 0);
    }

    /**
     * 구조화된 분석 결과를 judge의 3-String 계약(질문/기준/답변)에 태우기 위한 평탄화.
     * 필드 라벨을 고정해 rubric이 "근거 발견: 아니오" 같은 표현을 안정적으로 참조할 수 있게 한다.
     */
    private String flatten(MonitorAnalysis analysis) {
        final StringBuilder sb = new StringBuilder();
        sb.append("요약: ").append(analysis.summary()).append('\n');
        sb.append("근거 발견: ").append(analysis.evidenceFound() ? "예" : "아니오").append('\n');
        sb.append("원인 가설: ")
                .append(analysis.rootCauseHypothesis().isBlank() ? "(없음)" : analysis.rootCauseHypothesis())
                .append('\n');
        if (analysis.suggestedActions().isEmpty()) {
            sb.append("제안 조치: (없음)");
        } else {
            sb.append("제안 조치:");
            for (String action : analysis.suggestedActions()) {
                sb.append("\n- ").append(action);
            }
        }
        return sb.toString();
    }
}
