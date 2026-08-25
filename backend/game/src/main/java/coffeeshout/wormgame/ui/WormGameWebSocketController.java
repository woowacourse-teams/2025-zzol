package coffeeshout.wormgame.ui;

import coffeeshout.websocket.docs.WsReceive;
import coffeeshout.wormgame.infra.messaging.WormGameCommandPublisher;
import coffeeshout.wormgame.ui.request.SteerCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class WormGameWebSocketController {

    private final WormGameCommandPublisher commandPublisher;

    @MessageMapping("/room/{joinCode}/worm/steer")
    @WsReceive(
            respondsOnTopics = {"/room/{joinCode}/worm"},
            description = "지렁이 게임 조향 — 목표각(라디안)과 일련번호. 10Hz·변화 시만 전송")
    public void steer(@DestinationVariable String joinCode, @Payload @Valid SteerCommand command) {
        commandPublisher.steer(joinCode, command.playerName(), command.angle(), command.seq());
    }
}
