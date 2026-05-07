package coffeeshout.global.zzolbot.domain;

import java.util.List;
import java.util.Map;

public sealed interface ZzolBotLlmResponse
        permits ZzolBotLlmResponse.TextResponse, ZzolBotLlmResponse.ToolCallsResponse {

    record TextResponse(String text) implements ZzolBotLlmResponse {}

    record ToolCallsResponse(List<ToolCallItem> calls) implements ZzolBotLlmResponse {
        public record ToolCallItem(String toolName, Map<String, Object> args) {}
    }
}
