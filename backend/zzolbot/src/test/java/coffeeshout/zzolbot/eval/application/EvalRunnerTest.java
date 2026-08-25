package coffeeshout.zzolbot.eval.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import coffeeshout.zzolbot.config.ZzolBotProperties;
import coffeeshout.zzolbot.eval.domain.EvalRunStatus;
import coffeeshout.zzolbot.eval.domain.EvalVerdict;
import coffeeshout.zzolbot.eval.domain.JudgeScore;
import coffeeshout.zzolbot.eval.domain.ScenarioKind;
import coffeeshout.zzolbot.eval.domain.ScenarioSource;
import coffeeshout.zzolbot.eval.infra.EvalResultEntity;
import coffeeshout.zzolbot.eval.infra.EvalResultRepository;
import coffeeshout.zzolbot.eval.infra.EvalRunEntity;
import coffeeshout.zzolbot.eval.infra.EvalRunRepository;
import coffeeshout.zzolbot.eval.infra.EvalScenarioEntity;
import coffeeshout.zzolbot.eval.infra.EvalScenarioRepository;
import coffeeshout.zzolbot.eval.infra.JudgeClient;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EvalRunnerTest {

    private static final ZzolBotProperties PROPERTIES = new ZzolBotProperties(
            "test-key",
            "gemini-2.5-flash",
            5,
            new ZzolBotProperties.MonitoringProperties("http://loki", "http://tempo", "http://prom", "local"),
            new ZzolBotProperties.DeterminismProperties(0.1, 0.1),
            60,
            10000L,
            new ZzolBotProperties.SqlProperties(List.of(), 100, 3));

    @Mock
    private EvalScenarioRepository scenarioRepository;

    @Mock
    private EvalRunRepository runRepository;

    @Mock
    private EvalResultRepository resultRepository;

    @Mock
    private JudgeClient judgeClient;

    @Mock
    private ScenarioEvaluator chatEvaluator;

    private EvalRunner runner;

    @BeforeEach
    void setUp() {
        runner = new EvalRunner(
                scenarioRepository, runRepository, resultRepository, judgeClient, PROPERTIES, List.of(chatEvaluator));

        given(runRepository.save(any())).willAnswer(invocation -> {
            final EvalRunEntity run = invocation.getArgument(0);
            if (run.getId() == null) {
                ReflectionTestUtils.setField(run, "id", 7L);
            }
            return run;
        });
        given(chatEvaluator.kind()).willReturn(ScenarioKind.CHAT);
        given(chatEvaluator.evaluate(any())).willReturn(new EvalAnswer("진단 답변", 0));
    }

    @Test
    void 모든_시나리오를_채점해_결과를_저장하고_run을_완료한다() {
        given(scenarioRepository.findAllByOrderByCreatedAtDesc()).willReturn(List.of(scenario(1L), scenario(2L)));
        given(judgeClient.evaluate(anyString(), anyString(), anyString()))
                .willReturn(new JudgeScore(5, 5, false, EvalVerdict.PASS, "정답"))
                .willReturn(new JudgeScore(1, 1, true, EvalVerdict.FAIL, "오답"));

        final EvalRunEntity run = runner.run("baseline");

        final ArgumentCaptor<EvalResultEntity> captor = ArgumentCaptor.forClass(EvalResultEntity.class);
        verify(resultRepository, times(2)).save(captor.capture());
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(run.getStatus()).isEqualTo(EvalRunStatus.COMPLETED);
            softly.assertThat(run.getPassCount()).isEqualTo(1);
            softly.assertThat(run.getScenarioCount()).isEqualTo(2);
        });
    }

    @Test
    void 시나리오가_없으면_빈_run을_완료한다() {
        given(scenarioRepository.findAllByOrderByCreatedAtDesc()).willReturn(List.of());

        final EvalRunEntity run = runner.run("empty");

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(run.getStatus()).isEqualTo(EvalRunStatus.COMPLETED);
            softly.assertThat(run.getPassCount()).isZero();
        });
        verify(resultRepository, times(0)).save(any());
    }

    @Test
    void kind를_지정하면_해당_kind_시나리오만_실행한다() {
        given(scenarioRepository.findAllByKindOrderByCreatedAtDesc(ScenarioKind.CHAT))
                .willReturn(List.of(scenario(1L)));
        given(judgeClient.evaluate(anyString(), anyString(), anyString()))
                .willReturn(new JudgeScore(5, 5, false, EvalVerdict.PASS, "정답"));

        final EvalRunEntity run = runner.run("chat-only", 1, ScenarioKind.CHAT);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(run.getScenarioCount()).isEqualTo(1);
            softly.assertThat(run.getPassCount()).isEqualTo(1);
        });
        verify(scenarioRepository, times(0)).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void 평가기가_없는_kind의_시나리오는_FAIL_결과로_기록된다() {
        final EvalScenarioEntity monitorScenario = EvalScenarioEntity.create(
                "monitor-1", ScenarioKind.MONITOR, "알림", "{}", "rubric", ScenarioSource.RECORDED);
        ReflectionTestUtils.setField(monitorScenario, "id", 3L);
        given(scenarioRepository.findAllByOrderByCreatedAtDesc()).willReturn(List.of(monitorScenario));

        final EvalRunEntity run = runner.run("no-evaluator");

        final ArgumentCaptor<EvalResultEntity> captor = ArgumentCaptor.forClass(EvalResultEntity.class);
        verify(resultRepository, times(1)).save(captor.capture());
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(run.getStatus()).isEqualTo(EvalRunStatus.COMPLETED);
            softly.assertThat(run.getPassCount()).isZero();
            softly.assertThat(captor.getValue().getVerdict()).isEqualTo(EvalVerdict.FAIL);
        });
    }

    private EvalScenarioEntity scenario(Long id) {
        final EvalScenarioEntity entity = EvalScenarioEntity.create(
                "scenario-" + id, ScenarioKind.CHAT, "질문 " + id, "[]", "rubric", ScenarioSource.MANUAL);
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
