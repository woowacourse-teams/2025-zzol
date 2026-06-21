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

    public EvalRunEntity run(String label) {
        final List<EvalScenarioEntity> scenarios = scenarioRepository.findAllByOrderByCreatedAtDesc();
        final EvalRunEntity run = runRepository.save(
                EvalRunEntity.start(label, properties.model(), null, scenarios.size()));

        int passCount = 0;
        for (EvalScenarioEntity scenario : scenarios) {
            try {
                if (evaluateOne(run.getId(), scenario)) {
                    passCount++;
                }
            } catch (Exception e) {
                log.warn("[ZzolBot] 시나리오 평가 실패. scenario={}", scenario.getName(), e);
            }
        }

        run.complete(passCount);
        return runRepository.save(run);
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
}
