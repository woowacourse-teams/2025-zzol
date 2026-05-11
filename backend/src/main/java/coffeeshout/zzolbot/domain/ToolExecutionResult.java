package coffeeshout.zzolbot.domain;

public record ToolExecutionResult(
        String toolName,
        String content,
        boolean success
) {

    public static ToolExecutionResult ok(String toolName, String content) {
        return new ToolExecutionResult(toolName, content, true);
    }

    public static ToolExecutionResult fail(String toolName, String errorMessage) {
        return new ToolExecutionResult(toolName, errorMessage, false);
    }
}
