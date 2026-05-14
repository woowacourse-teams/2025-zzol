package coffeeshout.minigame.ui;

import coffeeshout.global.websocket.docs.WsTopic;
import coffeeshout.minigame.ui.command.MiniGameCommand;
import coffeeshout.minigame.ui.command.MiniGameCommandDispatcher;
import coffeeshout.minigame.ui.request.MiniGameMessage;
import coffeeshout.minigame.ui.response.MiniGameStartMessage;
import coffeeshout.minigame.ui.response.MiniGameStateMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class MiniGameWebSocketController {

    private final MiniGameCommandDispatcher miniGameCommandDispatcher;
    private final ObjectMapper objectMapper;

    @MessageMapping("/room/{joinCode}/minigame/command")
    @WsTopic(
            path = "/room/{joinCode}/round",
            payload = MiniGameStartMessage.class,
            description = "StartMiniGameCommand 처리 시 발행"
    )
    @WsTopic(
            path = "/room/{joinCode}/gameState",
            payload = MiniGameStateMessage.class,
            description = "SelectCardCommand 처리 시 발행"
    )
    public void commandGame(@DestinationVariable String joinCode, @Payload MiniGameMessage command) {
        MiniGameCommand miniGameCommand = command.toCommand(objectMapper);
        miniGameCommandDispatcher.dispatch(joinCode, miniGameCommand);
    }
}
