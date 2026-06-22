package coffeeshout.zzolbot.eval.application;

import coffeeshout.zzolbot.application.ToolResultSource;
import coffeeshout.zzolbot.domain.AskContext;
import coffeeshout.zzolbot.domain.ToolExecutionResult;
import coffeeshout.zzolbot.domain.ZzolBotLlmResponse.ToolCallsResponse.ToolCallItem;
import coffeeshout.zzolbot.eval.domain.ToolSnapshot;
import java.util.List;

/**
 * 평가 replay용 도구 결과 공급자. 라이브 도구 대신 박제된 스냅샷에서 결과를 찾아 반환한다.
 * 스냅샷에 없는 호출은 실패로 처리하고 {@link #getMissingCount()}로 커버리지 갭을 노출한다.
 * 단일 평가 실행 내에서 순차 사용을 전제로 하며 스레드 안전하지 않다.
 */
public class SnapshotToolResultSource implements ToolResultSource {

    private final ToolSnapshot snapshot;
    private int missingCount;

    public SnapshotToolResultSource(ToolSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    @Override
    public List<ToolExecutionResult> executeAll(List<ToolCallItem> calls, AskContext ctx) {
        return calls.stream().map(this::resolve).toList();
    }

    private ToolExecutionResult resolve(ToolCallItem call) {
        return snapshot.find(call.toolName(), call.args())
                .map(content -> ToolExecutionResult.ok(call.toolName(), content))
                .orElseGet(() -> {
                    missingCount++;
                    return ToolExecutionResult.fail(call.toolName(), "스냅샷에 없는 도구 호출: " + call.toolName());
                });
    }

    public int getMissingCount() {
        return missingCount;
    }
}
