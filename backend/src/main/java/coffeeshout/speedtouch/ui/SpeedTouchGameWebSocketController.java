package coffeeshout.speedtouch.ui;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.redis.stream.StreamKey;
import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.speedtouch.domain.event.TouchProgressCommandEvent;
import coffeeshout.speedtouch.ui.request.TouchCommand;
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
public class SpeedTouchGameWebSocketController {

    private final StreamPublisher streamPublisher;

    @MessageMapping("/room/{joinCode}/speed-touch/touch")
    @Operation(
            summary = "스피드 터치 게임 터치",
            description = "1 to 25 스피드 터치 게임에서 플레이어가 숫자를 터치하는 웹소켓 요청입니다."
    )
    @MessageResponse(
            path = "/topic/room/{joinCode}/speed-touch/progress",
            returnType = Object.class
    )
    public void touch(@DestinationVariable String joinCode, @Payload @Valid TouchCommand command) {
        final BaseEvent event = TouchProgressCommandEvent.create(
                joinCode, command.playerName(), command.touchedNumber()
        );
        streamPublisher.publish(StreamKey.SPEED_TOUCH_EVENTS, event);
        log.debug("터치 이벤트 발행: joinCode={}, player={}, number={}, eventId={}",
                joinCode, command.playerName(), command.touchedNumber(), event.eventId());
    }
}
