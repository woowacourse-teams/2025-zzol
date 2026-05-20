package coffeeshout.zzolbot.domain;

import java.util.Map;

public sealed interface ZzolBotMessage
        permits ZzolBotMessage.UserMessage,
                ZzolBotMessage.AssistantMessage,
                ZzolBotMessage.ToolCallMessage,
                ZzolBotMessage.ToolResultMessage {

    record UserMessage(String text) implements ZzolBotMessage {}

    record AssistantMessage(String text) implements ZzolBotMessage {}

    record ToolCallMessage(String toolName, Map<String, Object> args) implements ZzolBotMessage {}

    record ToolResultMessage(String toolName, String result) implements ZzolBotMessage {}
}
