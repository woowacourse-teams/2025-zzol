package coffeeshout.wormgame.ui;

import coffeeshout.websocket.PlayerKey;
import coffeeshout.websocket.docs.WsReceive;
import coffeeshout.wormgame.infra.messaging.WormGameCommandPublisher;
import coffeeshout.wormgame.ui.request.SteerCommand;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class WormGameWebSocketController {

    private final WormGameCommandPublisher commandPublisher;

    /**
     * 조향 대상은 STOMP principal 에서 도출한다 — 본문으로 받으면 남의 이름을 실어 그 사람 지렁이를
     * 조종할 수 있다. 항상 principal 과 같아야 하는 값이라 대조하지 않고 아예 본문에서 뺐다.
     */
    @MessageMapping("/room/{joinCode}/worm/steer")
    @WsReceive(
            respondsOnTopics = {"/room/{joinCode}/worm"},
            description = "지렁이 게임 조향 — 목표각(라디안)과 일련번호. 10Hz·변화 시만 전송")
    public void steer(@DestinationVariable String joinCode, @Payload @Valid SteerCommand command, Principal principal) {
        final String authenticatedPlayerName =
                PlayerKey.parse(principal.getName()).playerName();
        commandPublisher.steer(joinCode, authenticatedPlayerName, command.angle(), command.seq());
    }
}
