package coffeeshout.minigame.ui;

import coffeeshout.websocket.PlayerKey;
import coffeeshout.websocket.docs.WsReceive;
import coffeeshout.minigame.ui.command.MiniGameCommand;
import coffeeshout.minigame.ui.command.MiniGameCommandDispatcher;
import coffeeshout.minigame.ui.request.MiniGameMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.Principal;
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
    public void commandGame(
            @DestinationVariable String joinCode,
            @Payload MiniGameMessage command,
            Principal principal
    ) {
        final MiniGameCommand miniGameCommand = command.toCommand(objectMapper);
        final PlayerKey playerKey = PlayerKey.requireFrom(principal);
        miniGameCommandDispatcher.dispatch(joinCode, miniGameCommand, playerKey);
    }
}
