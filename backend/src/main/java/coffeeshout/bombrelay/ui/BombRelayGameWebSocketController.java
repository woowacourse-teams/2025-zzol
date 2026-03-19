package coffeeshout.bombrelay.ui;

import coffeeshout.bombrelay.domain.event.WordCommandEvent;
import coffeeshout.bombrelay.ui.request.WordCommand;
import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.redis.stream.StreamKey;
import coffeeshout.global.redis.stream.StreamPublisher;
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
public class BombRelayGameWebSocketController {

    private final StreamPublisher streamPublisher;

    @MessageMapping("/room/{joinCode}/bomb-relay/word")
    public void submitWord(@DestinationVariable String joinCode, @Payload @Valid WordCommand command) {
        final BaseEvent event = WordCommandEvent.create(joinCode, command.playerName(), command.word());
        streamPublisher.publish(StreamKey.BOMB_RELAY_EVENTS, event);
        log.debug("단어 입력 이벤트 발행: joinCode={}, player={}, word={}, eventId={}",
                joinCode, command.playerName(), command.word(), event.eventId());
    }
}
