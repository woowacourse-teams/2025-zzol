package coffeeshout.racinggame.ui;

import coffeeshout.racinggame.infra.messaging.RacingGameCommandPublisher;
import coffeeshout.racinggame.ui.request.TapCommand;
import coffeeshout.websocket.PlayerKey;
import coffeeshout.websocket.docs.WsReceive;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class RacingGameWebSocketController {

    private final RacingGameCommandPublisher racingGameCommandPublisher;

    @MessageMapping("/room/{joinCode}/racing-game/tap")
    @WsReceive(
            respondsOnTopics = {"/room/{joinCode}/racing-game/state"},
            description = "레이싱 게임 탭")
    public void tap(@DestinationVariable String joinCode, @Payload @Valid TapCommand command, Principal principal) {
        final String authenticatedPlayerName =
                PlayerKey.parse(principal.getName()).playerName();
        racingGameCommandPublisher.tap(joinCode, authenticatedPlayerName, command.tapCount());
    }
}
