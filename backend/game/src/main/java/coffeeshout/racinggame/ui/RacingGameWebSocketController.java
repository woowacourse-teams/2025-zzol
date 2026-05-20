package coffeeshout.racinggame.ui;

import coffeeshout.racinggame.application.RacingGameFacade;
import coffeeshout.racinggame.ui.request.TapCommand;
import coffeeshout.websocket.docs.WsReceive;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class RacingGameWebSocketController {

    private final RacingGameFacade racingGameFacade;

    @MessageMapping("/room/{joinCode}/racing-game/tap")
    @WsReceive(
            respondsOnTopics = {"/room/{joinCode}/racing-game/state"},
            description = "레이싱 게임 탭"
    )
    public void tap(@DestinationVariable String joinCode, @Payload @Valid TapCommand command) {
        racingGameFacade.tap(joinCode, command.playerName(), command.tapCount());
    }
}
