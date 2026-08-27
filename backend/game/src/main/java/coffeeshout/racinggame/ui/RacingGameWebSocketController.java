package coffeeshout.racinggame.ui;

import coffeeshout.racinggame.infra.messaging.RacingGameCommandPublisher;
import coffeeshout.racinggame.ui.request.TapCommand;
import coffeeshout.websocket.PlayerKey;
import coffeeshout.websocket.docs.WsReceive;
import jakarta.validation.Valid;
import java.security.Principal;
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

    /**
     * 탭 주인은 STOMP principal 에서 도출한다 — 본문으로 받으면 남의 이름을 실어 그 사람 러너를 대신
     * 달리게 할 수 있다. 항상 principal 과 같아야 하는 값이라 대조하지 않고 아예 본문에서 뺐다.
     */
    @MessageMapping("/room/{joinCode}/racing-game/tap")
    @WsReceive(
            respondsOnTopics = {"/room/{joinCode}/racing-game/state"},
            description = "레이싱 게임 탭"
    )
    public void tap(
            @DestinationVariable String joinCode,
            @Payload @Valid TapCommand command,
            Principal principal
    ) {
        final String authenticatedPlayerName = PlayerKey.parse(principal.getName()).playerName();
        racingGameCommandPublisher.tap(joinCode, authenticatedPlayerName, command.tapCount());
    }
}
