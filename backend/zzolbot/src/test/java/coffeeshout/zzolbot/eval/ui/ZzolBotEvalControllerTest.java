package coffeeshout.zzolbot.eval.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import coffeeshout.zzolbot.eval.application.EvalRunner;
import coffeeshout.zzolbot.eval.application.EvalScenarioService;
import coffeeshout.zzolbot.eval.domain.EvalVerdict;
import coffeeshout.zzolbot.eval.domain.JudgeScore;
import coffeeshout.zzolbot.eval.infra.EvalResultEntity;
import coffeeshout.zzolbot.eval.infra.EvalResultRepository;
import coffeeshout.zzolbot.eval.infra.EvalRunEntity;
import coffeeshout.zzolbot.eval.infra.EvalRunRepository;
import coffeeshout.zzolbot.eval.ui.request.RunRequest;
import coffeeshout.zzolbot.eval.ui.response.RunDetailResponse;
import coffeeshout.zzolbot.eval.ui.response.RunResponse;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ZzolBotEvalControllerTest {

    @Mock
    private EvalRunner evalRunner;
    @Mock
    private EvalScenarioService scenarioService;
    @Mock
    private EvalRunRepository runRepository;
    @Mock
    private EvalResultRepository resultRepository;

    private ZzolBotEvalController controller;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        executor = Executors.newVirtualThreadPerTaskExecutor();
        controller = new ZzolBotEvalController(
                evalRunner, scenarioService, runRepository, resultRepository, executor, Clock.systemDefaultZone());
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void startRun_은_202를_반환하고_평가를_비동기로_실행한다() {
        final var response = controller.startRun(new RunRequest("baseline", null));

        assertThat(response.getStatusCode().value()).isEqualTo(202);
        await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> verify(evalRunner).run("baseline", 1));
    }

    @Test
    void runs_는_실행_목록을_RunResponse로_변환한다() {
        final EvalRunEntity run = EvalRunEntity.start("baseline", "gemini-2.5-flash", null, 2);
        run.complete(1);
        ReflectionTestUtils.setField(run, "id", 7L);
        given(runRepository.findTop20ByOrderByStartedAtDesc()).willReturn(List.of(run));

        final List<RunResponse> result = controller.runs();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).hasSize(1);
            softly.assertThat(result.get(0).id()).isEqualTo(7L);
            softly.assertThat(result.get(0).passCount()).isEqualTo(1);
            softly.assertThat(result.get(0).status()).isEqualTo("COMPLETED");
        });
    }

    @Test
    void deleteScenario_는_시나리오를_삭제하고_204를_반환한다() {
        final var response = controller.deleteScenario(5L);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(scenarioService).delete(5L);
    }

    @Test
    void run_상세는_실행과_결과를_함께_반환한다() {
        final EvalRunEntity run = EvalRunEntity.start("baseline", "gemini-2.5-flash", null, 1);
        run.complete(1);
        ReflectionTestUtils.setField(run, "id", 7L);
        final EvalResultEntity entity = EvalResultEntity.create(
                7L, 3L, "PLAYING 상태입니다.",
                new JudgeScore(5, 4, false, EvalVerdict.PASS, "정답"), 1200L, 0);
        given(runRepository.findById(7L)).willReturn(Optional.of(run));
        given(resultRepository.findByRunIdOrderByIdAsc(7L)).willReturn(List.of(entity));

        final RunDetailResponse detail = controller.run(7L);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(detail.run().id()).isEqualTo(7L);
            softly.assertThat(detail.results()).hasSize(1);
            softly.assertThat(detail.results().get(0).verdict()).isEqualTo("PASS");
            softly.assertThat(detail.results().get(0).scenarioId()).isEqualTo(3L);
        });
    }
}
