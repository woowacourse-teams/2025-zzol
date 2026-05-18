package coffeeshout.blindtimer.ui;

import coffeeshout.blindtimer.domain.event.StopCommandEvent;
import coffeeshout.blindtimer.ui.request.StopCommand;
import coffeeshout.global.redis.BaseEvent;
import coffeeshout.blindtimer.infra.BlindTimerStreamKey;
import coffeeshout.global.redis.stream.StreamPublisher;
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
public class BlindTimerGameWebSocketController {

    private final StreamPublisher streamPublisher;

    @MessageMapping("/room/{joinCode}/blind-timer/stop")
    @WsReceive(
            respondsOnTopics = "/room/{joinCode}/blind-timer/progress",
            description = "블라인드 타이머 게임 STOP 버튼"
    )
    public void stop(@DestinationVariable String joinCode, @Payload @Valid StopCommand command) {
        final BaseEvent event = StopCommandEvent.create(joinCode, command.playerName());
        streamPublisher.publish(BlindTimerStreamKey.EVENTS, event);
        log.debug("STOP 이벤트 발행: joinCode={}, player={}, eventId={}",
                joinCode, command.playerName(), event.eventId());
    }
}
