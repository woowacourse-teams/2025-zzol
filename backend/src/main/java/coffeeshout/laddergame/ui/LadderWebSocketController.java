package coffeeshout.laddergame.ui;

import coffeeshout.laddergame.infra.LadderGameStreamKey;
import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.global.websocket.PlayerKey;
import coffeeshout.global.websocket.docs.WsReceive;
import coffeeshout.laddergame.domain.event.LadderDrawCommandEvent;
import coffeeshout.laddergame.ui.request.LadderDrawRequest;
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
public class LadderWebSocketController {

    private final StreamPublisher streamPublisher;

    @MessageMapping("/room/{joinCode}/ladder/draw")
    @WsReceive(respondsOnTopics = "/room/{joinCode}/ladder/line",
            description = "사다리 선 그리기 이벤트 발행 — 선 브로드캐스트")
    public void draw(
            @DestinationVariable String joinCode,
            @Payload @Valid LadderDrawRequest request,
            Principal principal
    ) {
        final String playerName = PlayerKey.parse(principal.getName()).playerName();
        final LadderDrawCommandEvent event = LadderDrawCommandEvent.of(joinCode, playerName, request.segmentIndex());
        streamPublisher.publish(LadderGameStreamKey.EVENTS, event);

        log.debug("사다리게임 선 그리기 이벤트 발행: joinCode={}, playerName={}, segmentIndex={}, eventId={}",
                joinCode, playerName, request.segmentIndex(), event.eventId());
    }
}
