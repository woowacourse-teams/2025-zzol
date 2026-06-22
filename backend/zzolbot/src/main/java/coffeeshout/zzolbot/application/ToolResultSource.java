package coffeeshout.zzolbot.application;

import coffeeshout.zzolbot.domain.AskContext;
import coffeeshout.zzolbot.domain.ToolExecutionResult;
import coffeeshout.zzolbot.domain.ZzolBotLlmResponse.ToolCallsResponse.ToolCallItem;
import java.util.List;

/**
 * 진단 루프가 도구 결과를 얻는 지점을 추상화한다.
 * 운영(라이브)에서는 {@link ToolExecutor}가 실제 도구를 실행하고,
 * 평가(replay)에서는 박제된 스냅샷이 결과를 공급한다.
 */
@FunctionalInterface
public interface ToolResultSource {

    List<ToolExecutionResult> executeAll(List<ToolCallItem> calls, AskContext ctx);
}
