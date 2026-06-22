package coffeeshout.zzolbot.eval.application;

import coffeeshout.zzolbot.application.SessionSink;
import coffeeshout.zzolbot.application.ZzolBotChatService;
import coffeeshout.zzolbot.config.ZzolBotProperties;
import coffeeshout.zzolbot.domain.ZzolBotChatResult;
import coffeeshout.zzolbot.eval.domain.EvalVerdict;
import coffeeshout.zzolbot.eval.domain.JudgeScore;
import coffeeshout.zzolbot.eval.domain.ToolSnapshot;
import coffeeshout.zzolbot.eval.infra.EvalResultEntity;
import coffeeshout.zzolbot.eval.infra.EvalResultRepository;
import coffeeshout.zzolbot.eval.infra.EvalRunEntity;
import coffeeshout.zzolbot.eval.infra.EvalRunRepository;
import coffeeshout.zzolbot.eval.infra.EvalScenarioEntity;
import coffeeshout.zzolbot.eval.infra.EvalScenarioRepository;
import coffeeshout.zzolbot.eval.infra.JudgeClient;
import coffeeshout.zzolbot.eval.infra.ToolSnapshotCodec;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 골든 시나리오를 일괄 실행해 채점·저장한다.
 * 각 시나리오는 박제된 도구 스냅샷을 replay하고(라이브 데이터 변동 제거) LLM 추론은 라이브로 호출해
 * 프롬프트/모델 변경의 A/B 비교가 성립하게 한다.
 * judge 레이트리밋과 무료 티어 보호를 위해 시나리오는 순차 실행한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvalRunner {

    private static final Consumer<String> NO_PROGRESS = toolName -> {
    };
    private static final SessionSink NON_PERSIST =
            (maskedQuestion, maskedAnswer, adminUsername, ctx) -> new ZzolBotChatResult(null, maskedAnswer);

    private final EvalScenarioRepository scenarioRepository;
    private final EvalRunRepository runRepository;
    private final EvalResultRepository resultRepository;
    private final ZzolBotChatService chatService;
    private final JudgeClient judgeClient;
    private final ToolSnapshotCodec codec;
    private final ZzolBotProperties properties;

    private static final int MAX_REPEATS = 3;

    public EvalRunEntity run(String label) {
        return run(label, 1);
    }

    /**
     * 각 시나리오를 최대 {@code repeats}회까지 평가한다. PASS가 한 번 나오면 거기서 멈추고,
     * 모든 시도가 FAIL일 때만 FAIL로 본다 — LLM 변동으로 가끔 빗나가는 걸 재시도로 완화한다.
     * repeats는 1~{@value #MAX_REPEATS}로 클램프한다(기본 1이면 비용·동작 모두 종전과 동일).
     */
    public EvalRunEntity run(String label, int repeats) {
        final int attempts = Math.max(1, Math.min(MAX_REPEATS, repeats));
        final List<EvalScenarioEntity> scenarios = scenarioRepository.findAllByOrderByCreatedAtDesc();
        final EvalRunEntity run = runRepository.save(
                EvalRunEntity.start(label, properties.model(), null, scenarios.size()));

        try {
            int passCount = 0;
            for (EvalScenarioEntity scenario : scenarios) {
                if (passesWithinRetries(run.getId(), scenario, attempts)) {
                    passCount++;
                }
            }
            run.complete(passCount);
            return runRepository.save(run);
        } catch (Exception e) {
            log.error("[ZzolBot] 평가 실행 실패 — run을 FAILED로 마킹. runId={}", run.getId(), e);
            run.fail();
            return runRepository.save(run);
        }
    }

    /**
     * 한 시나리오를 최대 attempts회까지 평가한다(각 시도는 결과 1행으로 저장).
     * PASS가 한 번 나오면 즉시 멈추고(토큰 절약), 모든 시도가 FAIL일 때만 FAIL로 본다 —
     * LLM 변동으로 가끔 빗나가는 걸 재시도로 흡수한다.
     */
    private boolean passesWithinRetries(Long runId, EvalScenarioEntity scenario, int attempts) {
        for (int i = 0; i < attempts; i++) {
            try {
                if (evaluateOne(runId, scenario)) {
                    return true; // 통과하면 즉시 멈춤
                }
            } catch (Exception e) {
                log.warn("[ZzolBot] 시나리오 평가 실패 — FAIL 결과로 기록. scenario={}, attempt={}",
                        scenario.getName(), i + 1, e);
                saveFailureResult(runId, scenario, e);
            }
        }
        return false; // 모든 시도 FAIL
    }

    private boolean evaluateOne(Long runId, EvalScenarioEntity scenario) {
        final ToolSnapshot snapshot = codec.fromJson(scenario.getSnapshotJson());
        final SnapshotToolResultSource source = new SnapshotToolResultSource(snapshot);

        final long startNanos = System.nanoTime();
        final ZzolBotChatResult result = chatService.ask(
                scenario.getQuestion(), "eval", NO_PROGRESS, source, NON_PERSIST);
        final long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;

        final JudgeScore score = judgeClient.evaluate(scenario.getQuestion(), scenario.getRubric(), result.answer());
        resultRepository.save(EvalResultEntity.create(
                runId, scenario.getId(), result.answer(), score, latencyMs, source.missingCount()));

        return score.verdict() == EvalVerdict.PASS;
    }

    /**
     * 시나리오 평가가 예외로 끝났을 때도 FAIL 결과를 남겨 "시도당 결과 1건" 불변식을 유지한다.
     * (그래야 실행 상세에 빠진 시도 없이 모든 시도가 한 행씩 남는다.)
     */
    private void saveFailureResult(Long runId, EvalScenarioEntity scenario, Exception e) {
        final JudgeScore failed = new JudgeScore(0, 0, false, EvalVerdict.FAIL, "평가 중 예외: " + e.getMessage());
        resultRepository.save(EvalResultEntity.create(
                runId, scenario.getId(), "평가 실패", failed, 0L, 0));
    }
}
