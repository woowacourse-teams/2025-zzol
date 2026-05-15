package coffeeshout.minigame.ui;

import coffeeshout.global.websocket.docs.WsReceive;
import coffeeshout.minigame.ui.command.MiniGameCommand;
import coffeeshout.minigame.ui.command.MiniGameCommandDispatcher;
import coffeeshout.minigame.ui.request.MiniGameMessage;
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
    @WsReceive(
            respondsOnTopics = {"/room/{joinCode}/round", "/room/{joinCode}/gameState"},
            description = "StartMiniGameCommand → round 발행, SelectCardCommand → gameState 발행"
    )
    public void commandGame(@DestinationVariable String joinCode, @Payload MiniGameMessage command) {
        final MiniGameCommand miniGameCommand = command.toCommand(objectMapper);
        miniGameCommandDispatcher.dispatch(joinCode, miniGameCommand);
    }
}
