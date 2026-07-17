package coffeeshout.minigame.ui.request;

import coffeeshout.minigame.ui.command.MiniGameCommand;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public record MiniGameMessage(
        CommandType commandType,
        JsonNode commandRequest
) {
    public MiniGameCommand toCommand(@Autowired ObjectMapper objectMapper) {
        return commandType.toCommandRequest(objectMapper, commandRequest);
    }
}
