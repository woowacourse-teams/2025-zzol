package coffeeshout.zzolbot.application;

import coffeeshout.zzolbot.config.ZzolBotProperties;
import coffeeshout.zzolbot.domain.AskContext;
import coffeeshout.zzolbot.domain.ToolExecutionResult;
import coffeeshout.zzolbot.domain.ZzolBotLlmResponse.ToolCallsResponse.ToolCallItem;
import coffeeshout.zzolbot.domain.ZzolBotTool;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ToolExecutor implements ToolResultSource {

    private final Map<String, ZzolBotTool> toolsByName;
    private final ExecutorService virtualExecutor;
    private final long toolTimeoutMillis;

    public ToolExecutor(List<ZzolBotTool> tools, ZzolBotProperties properties) {
        this.toolsByName = tools.stream()
                .collect(Collectors.toUnmodifiableMap(ZzolBotTool::name, t -> t));
        this.virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
        this.toolTimeoutMillis = properties.toolTimeoutMillis();
    }

    public List<ZzolBotTool> tools() {
        return List.copyOf(toolsByName.values());
    }

    @Override
    public List<ToolExecutionResult> executeAll(List<ToolCallItem> calls, AskContext ctx) {
        final List<CompletableFuture<ToolExecutionResult>> futures = calls.stream()
                .map(call -> CompletableFuture
                        .supplyAsync(() -> safeExecute(call, ctx), virtualExecutor)
                        .orTimeout(toolTimeoutMillis, TimeUnit.MILLISECONDS)
                        .exceptionally(e -> {
                            log.warn("[ZzolBot] tool 타임아웃 또는 예외. toolName={}", call.toolName(), e);
                            return ToolExecutionResult.fail(call.toolName(), "tool 실행 시간 초과: " + call.toolName());
                        }))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    private ToolExecutionResult safeExecute(ToolCallItem call, AskContext ctx) {
        try {
            final ZzolBotTool tool = toolsByName.get(call.toolName());
            if (tool == null) {
                return ToolExecutionResult.fail(call.toolName(), "알 수 없는 tool: " + call.toolName());
            }
            return tool.execute(call.args(), ctx);
        } catch (Exception e) {
            log.warn("[ZzolBot] tool 실행 중 예외 발생. toolName={}", call.toolName(), e);
            return ToolExecutionResult.fail(call.toolName(), "tool 실행 실패: " + call.toolName());
        }
    }

    @PreDestroy
    public void shutdown() {
        virtualExecutor.shutdown();
    }
}
