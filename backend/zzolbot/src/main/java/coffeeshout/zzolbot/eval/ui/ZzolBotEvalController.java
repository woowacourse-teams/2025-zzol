package coffeeshout.zzolbot.eval.ui;

import coffeeshout.zzolbot.eval.application.EvalRunner;
import coffeeshout.zzolbot.eval.application.EvalScenarioService;
import coffeeshout.zzolbot.eval.infra.EvalResultEntity;
import coffeeshout.zzolbot.eval.infra.EvalResultRepository;
import coffeeshout.zzolbot.eval.infra.EvalRunEntity;
import coffeeshout.zzolbot.eval.infra.EvalRunRepository;
import coffeeshout.zzolbot.eval.infra.EvalScenarioEntity;
import coffeeshout.zzolbot.eval.ui.request.ManualScenarioRequest;
import coffeeshout.zzolbot.eval.ui.request.RecordScenarioRequest;
import coffeeshout.zzolbot.eval.ui.request.RunRequest;
import coffeeshout.zzolbot.eval.ui.response.ResultResponse;
import coffeeshout.zzolbot.eval.ui.response.RunDetailResponse;
import coffeeshout.zzolbot.eval.ui.response.RunResponse;
import coffeeshout.zzolbot.eval.ui.response.ScenarioResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
    // 평가는 비동기라 POST는 즉시 반환한다. 중복 실행(더블클릭·다중 탭)으로 LLM 호출이 배가되지 않도록 한 번에 하나만 허용한다.
    private final AtomicBoolean evalRunning = new AtomicBoolean(false);

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
        final int repeats = request.repeats() == null ? 1 : request.repeats();
        if (!evalRunning.compareAndSet(false, true)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // 이미 실행 중 — 중복 실행 차단
        }
        try {
            virtualThreadExecutor.execute(() -> {
                try {
                    evalRunner.run(request.label(), repeats);
                } catch (Exception e) {
                    log.warn("[ZzolBot] 평가 실행 실패. label={}", request.label(), e);
                } finally {
                    evalRunning.set(false);
                }
            });
        } catch (RejectedExecutionException e) {
            // 제출 실패 시 finally(워커 람다 안)에 도달하지 못하므로 여기서 가드를 풀어준다.
            evalRunning.set(false);
            log.warn("[ZzolBot] 평가 실행 제출 거부. label={}", request.label(), e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 평가 실행: " + id));
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
}
