package coffeeshout.zzolbot.domain;

import java.util.Map;

public interface ZzolBotTool {

    String name();

    String description();

    Map<String, Object> parameterSchema();

    ToolExecutionResult execute(Map<String, Object> params, AskContext ctx);
}
