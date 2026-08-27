package coffeeshout.speedtouch.ui;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.speedtouch.domain.event.TouchProgressCommandEvent;
import coffeeshout.speedtouch.infra.SpeedTouchStreamKey;
import coffeeshout.speedtouch.ui.request.TouchCommand;
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
public class SpeedTouchGameWebSocketController {

    private final StreamPublisher streamPublisher;

    /**
     * 터치 주인은 STOMP principal 에서 도출한다 — 본문으로 받으면 남의 이름을 실어 그 사람 진행도를
     * 대신 올릴 수 있다. 항상 principal 과 같아야 하는 값이라 대조하지 않고 아예 본문에서 뺐다.
     */
    @MessageMapping("/room/{joinCode}/speed-touch/touch")
    @WsReceive(
            respondsOnTopics = "/room/{joinCode}/speed-touch/progress",
            description = "스피드 터치 게임 터치 — 1 to 25 스피드 터치에서 숫자를 터치하는 웹소켓 요청"
    )
    public void touch(
            @DestinationVariable String joinCode,
            @Payload @Valid TouchCommand command,
            Principal principal
    ) {
        final String authenticatedPlayerName = PlayerKey.parse(principal.getName()).playerName();
        final BaseEvent event = TouchProgressCommandEvent.create(
                joinCode, authenticatedPlayerName, command.touchedNumber()
        );
        streamPublisher.publish(SpeedTouchStreamKey.EVENTS, event);
        log.debug("터치 이벤트 발행: joinCode={}, player={}, number={}, eventId={}",
                joinCode, authenticatedPlayerName, command.touchedNumber(), event.eventId());
    }
}
