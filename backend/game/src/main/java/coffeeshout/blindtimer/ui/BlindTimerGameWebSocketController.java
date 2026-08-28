package coffeeshout.blindtimer.ui;

import coffeeshout.blindtimer.domain.event.StopCommandEvent;
import coffeeshout.blindtimer.infra.BlindTimerStreamKey;
import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.websocket.PlayerKey;
import coffeeshout.websocket.docs.WsReceive;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class BlindTimerGameWebSocketController {

    private final StreamPublisher streamPublisher;

    @MessageMapping("/room/{joinCode}/blind-timer/stop")
    @WsReceive(respondsOnTopics = "/room/{joinCode}/blind-timer/progress", description = "블라인드 타이머 게임 STOP 버튼")
    public void stop(@DestinationVariable String joinCode, Principal principal) {
        final String authenticatedPlayerName =
                PlayerKey.parse(principal.getName()).playerName();
        final BaseEvent event = StopCommandEvent.create(joinCode, authenticatedPlayerName);
        streamPublisher.publish(BlindTimerStreamKey.EVENTS, event);
        log.debug(
                "STOP 이벤트 발행: joinCode={}, player={}, eventId={}", joinCode, authenticatedPlayerName, event.eventId());
    }
}
