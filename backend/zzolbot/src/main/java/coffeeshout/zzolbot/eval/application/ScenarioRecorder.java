package coffeeshout.zzolbot.eval.application;

import coffeeshout.zzolbot.application.ToolExecutor;
import coffeeshout.zzolbot.application.ToolResultSource;
import coffeeshout.zzolbot.application.ZzolBotChatService;
import coffeeshout.zzolbot.domain.PiiMasker;
import coffeeshout.zzolbot.domain.ToolExecutionResult;
import coffeeshout.zzolbot.domain.ZzolBotChatResult;
import coffeeshout.zzolbot.eval.domain.ToolCallKey;
import coffeeshout.zzolbot.eval.domain.ToolSnapshot;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 운영 진단 세션을 라이브로 1회 실행하면서 각 도구 결과를 녹화해 골든 시나리오의 스냅샷을 만든다.
 * 저장될 스냅샷은 PII 마스킹된 결과로 박제해 평가 DB에 원본 PII가 남지 않게 한다.
 */
@Component
@RequiredArgsConstructor
public class ScenarioRecorder {

    private final ZzolBotChatService chatService;
    private final ToolExecutor toolExecutor;
    private final PiiMasker piiMasker;

    public Recorded record(String question, String adminUsername) {
        final Map<ToolCallKey, String> captured = new LinkedHashMap<>();
        final ToolResultSource recording = (calls, ctx) -> {
            final List<ToolExecutionResult> results = toolExecutor.executeAll(calls, ctx);
            for (int i = 0; i < calls.size(); i++) {
                final String masked = piiMasker.mask(results.get(i).content(), ctx.piiSession());
                captured.put(ToolCallKey.of(calls.get(i).toolName(), calls.get(i).args()), masked);
            }
            return results;
        };

        final ZzolBotChatResult result = chatService.ask(
                question, adminUsername, toolName -> {
                }, recording, (q, a, admin, ctx) -> new ZzolBotChatResult(null, a));

        return new Recorded(result.answer(), new ToolSnapshot(captured));
    }

    public record Recorded(String answer, ToolSnapshot snapshot) {
    }
}
