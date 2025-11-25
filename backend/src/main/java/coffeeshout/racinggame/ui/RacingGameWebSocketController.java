package coffeeshout.racinggame.ui;

import coffeeshout.racinggame.application.RacingGameFacade;
import coffeeshout.racinggame.domain.event.RaceStateChangedEvent;
import coffeeshout.racinggame.ui.request.TapCommand;
import generator.annotaions.MessageResponse;
import generator.annotaions.Operation;
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
    @Operation(
            summary = "레이싱 게임 탭",
            description = "레이싱 게임에서 플레이어가 화면을 탭하는 웹소켓 요청입니다."
    )
    @MessageResponse(
            path = "/topic/room/{joinCode}/racing-game/state",
            returnType = RaceStateChangedEvent.class
    )
    public void tap(@DestinationVariable String joinCode, @Payload @Valid TapCommand command) {
        racingGameFacade.tap(joinCode, command.playerName(), command.tapCount());
    }
}
