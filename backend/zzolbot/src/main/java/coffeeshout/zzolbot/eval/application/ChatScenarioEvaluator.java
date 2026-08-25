package coffeeshout.zzolbot.eval.application;

import coffeeshout.zzolbot.application.SessionSink;
import coffeeshout.zzolbot.application.ZzolBotChatService;
import coffeeshout.zzolbot.domain.ZzolBotChatResult;
import coffeeshout.zzolbot.eval.domain.ScenarioKind;
import coffeeshout.zzolbot.eval.domain.ToolSnapshot;
import coffeeshout.zzolbot.eval.infra.EvalScenarioEntity;
import coffeeshout.zzolbot.eval.infra.ToolSnapshotCodec;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 챗봇(진단) 경로 평가기. 박제된 도구 스냅샷을 replay하고 LLM 추론만 라이브로 호출한다.
 * 답변은 세션에 저장하지 않는다(NON_PERSIST) — 평가가 운영 세션 이력을 오염시키지 않는다.
 */
@Component
@RequiredArgsConstructor
public class ChatScenarioEvaluator implements ScenarioEvaluator {

    private static final Consumer<String> NO_PROGRESS = toolName -> {};
    private static final SessionSink NON_PERSIST =
            (maskedQuestion, maskedAnswer, adminUsername, ctx) -> new ZzolBotChatResult(null, maskedAnswer);

    private final ZzolBotChatService chatService;
    private final ToolSnapshotCodec codec;

    @Override
    public ScenarioKind kind() {
        return ScenarioKind.CHAT;
    }

    @Override
    public EvalAnswer evaluate(EvalScenarioEntity scenario) {
        final ToolSnapshot snapshot = codec.fromJson(scenario.getSnapshotJson());
        final SnapshotToolResultSource source = new SnapshotToolResultSource(snapshot);
        final ZzolBotChatResult result =
                chatService.ask(scenario.getQuestion(), "eval", NO_PROGRESS, source, NON_PERSIST);
        return new EvalAnswer(result.answer(), source.getMissingCount());
    }
}
