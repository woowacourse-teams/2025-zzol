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

    /**
     * STOP 주인은 STOMP principal 에서 도출한다 — 본문으로 받으면 남의 이름을 실어 그 사람 타이머를
     * 대신 멈출 수 있다. playerName 을 빼고 나면 본문에 남는 게 없어 request DTO 자체를 두지 않는다.
     */
    @MessageMapping("/room/{joinCode}/blind-timer/stop")
    @WsReceive(
            respondsOnTopics = "/room/{joinCode}/blind-timer/progress",
            description = "블라인드 타이머 게임 STOP 버튼"
    )
    public void stop(@DestinationVariable String joinCode, Principal principal) {
        final String authenticatedPlayerName = PlayerKey.parse(principal.getName()).playerName();
        final BaseEvent event = StopCommandEvent.create(joinCode, authenticatedPlayerName);
        streamPublisher.publish(BlindTimerStreamKey.EVENTS, event);
        log.debug("STOP 이벤트 발행: joinCode={}, player={}, eventId={}",
                joinCode, authenticatedPlayerName, event.eventId());
    }
}
