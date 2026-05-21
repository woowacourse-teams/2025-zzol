package coffeeshout.racinggame.ui;

import coffeeshout.websocket.docs.WsReceive;
import coffeeshout.racinggame.infra.messaging.RacingGameCommandPublisher;
import coffeeshout.racinggame.ui.request.TapCommand;
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

    private final RacingGameCommandPublisher racingGameCommandPublisher;

    @MessageMapping("/room/{joinCode}/racing-game/tap")
    @WsReceive(
            respondsOnTopics = {"/room/{joinCode}/racing-game/state"},
            description = "레이싱 게임 탭"
    )
    public void tap(@DestinationVariable String joinCode, @Payload @Valid TapCommand command) {
        racingGameCommandPublisher.tap(joinCode, command.playerName(), command.tapCount());
    }
}
