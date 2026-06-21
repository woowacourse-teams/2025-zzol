package coffeeshout.zzolbot.eval.ui;

import coffeeshout.zzolbot.eval.application.EvalRunner;
import coffeeshout.zzolbot.eval.application.EvalScenarioService;
import coffeeshout.zzolbot.eval.infra.EvalResultEntity;
import coffeeshout.zzolbot.eval.infra.EvalResultRepository;
import coffeeshout.zzolbot.eval.infra.EvalRunEntity;
import coffeeshout.zzolbot.eval.infra.EvalRunRepository;
import coffeeshout.zzolbot.eval.infra.EvalScenarioEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.security.Principal;
import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 평가 하네스 어드민 API. 평가 실행은 시나리오 수만큼 LLM을 호출해 오래 걸리므로
 * 가상 스레드에서 비동기로 시작하고, 대시보드는 {@code GET /runs}로 진행/결과를 폴링한다.
 */
@Slf4j
@RestController
@Validated
@RequestMapping("/admin/zzolbot/eval")
public class ZzolBotEvalController {

    private final EvalRunner evalRunner;
    private final EvalScenarioService scenarioService;
    private final EvalRunRepository runRepository;
    private final EvalResultRepository resultRepository;
    private final ExecutorService virtualThreadExecutor;
    private final DateTimeFormatter formatter;

    public ZzolBotEvalController(
            EvalRunner evalRunner,
            EvalScenarioService scenarioService,
            EvalRunRepository runRepository,
            EvalResultRepository resultRepository,
            @Qualifier("virtualThreadExecutor") ExecutorService virtualThreadExecutor,
            Clock clock
    ) {
        this.evalRunner = evalRunner;
        this.scenarioService = scenarioService;
        this.runRepository = runRepository;
        this.resultRepository = resultRepository;
        this.virtualThreadExecutor = virtualThreadExecutor;
        this.formatter = DateTimeFormatter.ofPattern("MM/dd HH:mm").withZone(clock.getZone());
    }

    @PostMapping("/runs")
    public ResponseEntity<Void> startRun(@RequestBody @Valid RunRequest request) {
        virtualThreadExecutor.execute(() -> {
            try {
                evalRunner.run(request.label());
            } catch (Exception e) {
                log.warn("[ZzolBot] 평가 실행 실패. label={}", request.label(), e);
            }
        });
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/runs")
    public List<RunResponse> runs() {
        return runRepository.findTop20ByOrderByStartedAtDesc().stream()
                .map(this::toRunResponse)
                .toList();
    }

    @GetMapping("/runs/{id}")
    public RunDetailResponse run(@PathVariable Long id) {
        final EvalRunEntity run = runRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 평가 실행: " + id));
        final List<ResultResponse> results = resultRepository.findByRunIdOrderByIdAsc(id).stream()
                .map(this::toResultResponse)
                .toList();
        return new RunDetailResponse(toRunResponse(run), results);
    }

    @GetMapping("/scenarios")
    public List<ScenarioResponse> scenarios() {
        return scenarioService.list().stream()
                .map(this::toScenarioResponse)
                .toList();
    }

    @PostMapping("/scenarios")
    public ScenarioResponse registerManual(@RequestBody @Valid ManualScenarioRequest request) {
        return toScenarioResponse(scenarioService.registerManual(
                request.name(), request.question(), request.snapshotJson(), request.rubric()));
    }

    @PostMapping("/scenarios/record")
    public ScenarioResponse registerRecorded(@RequestBody @Valid RecordScenarioRequest request, Principal principal) {
        final String adminUsername = principal != null ? principal.getName() : "unknown";
        return toScenarioResponse(scenarioService.registerRecorded(
                request.name(), request.question(), request.rubric(), adminUsername));
    }

    @DeleteMapping("/scenarios/{id}")
    public ResponseEntity<Void> deleteScenario(@PathVariable Long id) {
        scenarioService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private RunResponse toRunResponse(EvalRunEntity run) {
        return new RunResponse(
                run.getId(),
                run.getLabel(),
                run.getModel(),
                run.getStatus().name(),
                run.getScenarioCount(),
                run.getPassCount(),
                formatter.format(run.getStartedAt()),
                run.getFinishedAt() != null ? formatter.format(run.getFinishedAt()) : null);
    }

    private ResultResponse toResultResponse(EvalResultEntity result) {
        return new ResultResponse(
                result.getScenarioId(),
                result.getAccuracy(),
                result.getGroundedness(),
                result.isHallucination(),
                result.getVerdict().name(),
                result.getLatencyMs(),
                result.getMissingToolCalls(),
                result.getRationale(),
                result.getAnswer());
    }

    private ScenarioResponse toScenarioResponse(EvalScenarioEntity scenario) {
        return new ScenarioResponse(
                scenario.getId(),
                scenario.getName(),
                scenario.getQuestion(),
                scenario.getRubric(),
                scenario.getSourceType().name(),
                formatNullable(scenario.getCreatedAt()));
    }

    private String formatNullable(Instant instant) {
        return instant != null ? formatter.format(instant) : null;
    }

    record RunRequest(@NotBlank String label) {}

    record ManualScenarioRequest(
            @NotBlank String name,
            @NotBlank String question,
            @NotBlank String snapshotJson,
            @NotBlank String rubric) {}

    record RecordScenarioRequest(
            @NotBlank String name,
            @NotBlank String question,
            @NotBlank String rubric) {}

    record RunResponse(Long id, String label, String model, String status,
                       int scenarioCount, int passCount, String startedAt, String finishedAt) {}

    record ResultResponse(Long scenarioId, int accuracy, int groundedness, boolean hallucination,
                          String verdict, long latencyMs, int missingToolCalls, String rationale, String answer) {}

    record RunDetailResponse(RunResponse run, List<ResultResponse> results) {}

    record ScenarioResponse(Long id, String name, String question, String rubric, String sourceType, String createdAt) {}
}
