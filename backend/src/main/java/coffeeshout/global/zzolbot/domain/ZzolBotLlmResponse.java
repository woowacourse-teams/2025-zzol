package coffeeshout.global.zzolbot.domain;

import java.util.Map;

public sealed interface ZzolBotLlmResponse
        permits ZzolBotLlmResponse.TextResponse, ZzolBotLlmResponse.ToolCallResponse {

    record TextResponse(String text) implements ZzolBotLlmResponse {}

    record ToolCallResponse(String toolName, Map<String, Object> args) implements ZzolBotLlmResponse {}
}
