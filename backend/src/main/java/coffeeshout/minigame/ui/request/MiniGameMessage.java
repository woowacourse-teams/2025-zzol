package coffeeshout.minigame.ui.request;

import coffeeshout.minigame.ui.command.MiniGameCommand;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;

public record MiniGameMessage(
        CommandType commandType,
        JsonNode commandRequest
) {
    public MiniGameCommand toCommand(@Autowired ObjectMapper objectMapper) {
        return commandType.toCommandRequest(objectMapper, commandRequest);
    }
}
