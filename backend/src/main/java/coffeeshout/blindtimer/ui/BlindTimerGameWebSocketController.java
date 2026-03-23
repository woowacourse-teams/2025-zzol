package coffeeshout.blindtimer.ui;

import coffeeshout.blindtimer.domain.event.StopCommandEvent;
import coffeeshout.blindtimer.ui.request.StopCommand;
import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.redis.stream.StreamKey;
import coffeeshout.global.redis.stream.StreamPublisher;
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
public class BlindTimerGameWebSocketController {

    private final StreamPublisher streamPublisher;

    @MessageMapping("/room/{joinCode}/blind-timer/stop")
    @Operation(
            summary = "블라인드 타이머 게임 STOP",
            description = "블라인드 타이머 게임에서 플레이어가 STOP 버튼을 누르는 웹소켓 요청입니다."
    )
    @MessageResponse(
            path = "/topic/room/{joinCode}/blind-timer/progress",
            returnType = Object.class
    )
    public void stop(@DestinationVariable String joinCode, @Payload @Valid StopCommand command) {
        final BaseEvent event = StopCommandEvent.create(joinCode, command.playerName());
        streamPublisher.publish(StreamKey.BLIND_TIMER_EVENTS, event);
        log.debug("STOP 이벤트 발행: joinCode={}, player={}, eventId={}",
                joinCode, command.playerName(), event.eventId());
    }
}
