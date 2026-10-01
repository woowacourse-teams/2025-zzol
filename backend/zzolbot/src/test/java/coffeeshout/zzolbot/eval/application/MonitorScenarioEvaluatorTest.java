package coffeeshout.zzolbot.eval.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import coffeeshout.zzolbot.eval.domain.MonitorScenarioFixture;
import coffeeshout.zzolbot.eval.domain.ScenarioKind;
import coffeeshout.zzolbot.eval.domain.ScenarioSource;
import coffeeshout.zzolbot.eval.infra.EvalScenarioEntity;
import coffeeshout.zzolbot.eval.infra.MonitorFixtureCodec;
import coffeeshout.zzolbot.monitor.domain.FiringAlert;
import coffeeshout.zzolbot.monitor.domain.MonitorAnalysis;
import coffeeshout.zzolbot.monitor.infra.AnomalyAnalyzer;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MonitorScenarioEvaluatorTest {

    private static final FiringAlert ALERT = new FiringAlert(
            "IpBanRateSpike", "warning", "fp-1", "신규 IP BAN 급증", "설명", Map.of("incident_group", "ip-blocking"));

    @Mock
    private AnomalyAnalyzer analyzer;

    @Mock
    private MonitorFixtureCodec codec;

    private MonitorScenarioEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new MonitorScenarioEvaluator(analyzer, codec);
    }

    @Test
    void kind는_MONITOR다() {
        assertThat(evaluator.kind()).isEqualTo(ScenarioKind.MONITOR);
    }

    @Test
    void 픽스처를_분석기에_주입해_분석_결과를_평탄화한다() {
        final MonitorScenarioFixture fixture = new MonitorScenarioFixture(ALERT, List.of("ERROR 로그 한 줄"), "prod");
        given(codec.fromJson("{}")).willReturn(fixture);
        given(analyzer.analyze(ALERT, List.of("ERROR 로그 한 줄"), "prod"))
                .willReturn(new MonitorAnalysis("스캐너 스윕으로 차단 급증", "악성 경로 접근", List.of("차단 IP 대역 확인"), true));

        final EvalAnswer answer = evaluator.evaluate(scenario());

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(answer.answer()).contains("요약: 스캐너 스윕으로 차단 급증");
            softly.assertThat(answer.answer()).contains("근거 발견: 예");
            softly.assertThat(answer.answer()).contains("원인 가설: 악성 경로 접근");
            softly.assertThat(answer.answer()).contains("- 차단 IP 대역 확인");
            softly.assertThat(answer.missingToolCalls()).isZero();
        });
    }

    @Test
    void 근거_없음으로_강등된_분석은_가설과_조치가_없음으로_표기된다() {
        final MonitorScenarioFixture fixture = new MonitorScenarioFixture(ALERT, List.of("무관한 로그"), "prod");
        given(codec.fromJson("{}")).willReturn(fixture);
        given(analyzer.analyze(ALERT, List.of("무관한 로그"), "prod"))
                .willReturn(new MonitorAnalysis("근거 없음", "", List.of(), false));

        final EvalAnswer answer = evaluator.evaluate(scenario());

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(answer.answer()).contains("근거 발견: 아니오");
            softly.assertThat(answer.answer()).contains("원인 가설: (없음)");
            softly.assertThat(answer.answer()).contains("제안 조치: (없음)");
        });
    }

    private EvalScenarioEntity scenario() {
        return EvalScenarioEntity.create(
                "monitor-1", ScenarioKind.MONITOR, "알림: IpBanRateSpike", "{}", "rubric", ScenarioSource.RECORDED);
    }
}
