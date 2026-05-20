package coffeeshout.blockstacking.ui;

import coffeeshout.blockstacking.domain.event.BlockStackingCommandEvent;
import coffeeshout.blockstacking.domain.event.BlockStackingFailEvent;
import coffeeshout.gamecommon.infra.GameStreamKey;
import coffeeshout.blockstacking.ui.request.BlockStackingProgressRequest;
import coffeeshout.redis.stream.StreamPublisher;
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
public class BlockStackingWebSocketController {

    private final StreamPublisher streamPublisher;

    @MessageMapping("/room/{joinCode}/block-stacking/progress")
    @WsReceive(
            respondsOnTopics = "/room/{joinCode}/block-stacking/progress",
            description = "블록 쌓기 진행 이벤트 — overlap 을 재계산하여 유효한 안착만 인정 후 랭킹 브로드캐스트"
    )
    public void recordProgress(
            @DestinationVariable String joinCode,
            @Payload @Valid BlockStackingProgressRequest request,
            Principal principal
    ) {
        final String authenticatedPlayerName = PlayerKey.parse(principal.getName()).playerName();
        final BlockStackingCommandEvent event = BlockStackingCommandEvent.of(joinCode, authenticatedPlayerName, request);
        streamPublisher.publish(GameStreamKey.BLOCKSTACKING_EVENTS, event);

        log.debug("블록 쌓기 진행 이벤트 발행: joinCode={}, playerName={}, floor={}, eventId = {}",
                joinCode, authenticatedPlayerName, request.floor(), event.eventId());
    }

    @MessageMapping("/room/{joinCode}/block-stacking/fail")
    @WsReceive(respondsOnTopics = "/room/{joinCode}/block-stacking/state",
            description = "블록 쌓기 실패 이벤트 — 상태 변경 브로드캐스트")
    public void recordFail(
            @DestinationVariable String joinCode,
            Principal principal
    ) {
        final String authenticatedPlayerName = PlayerKey.parse(principal.getName()).playerName();
        final BlockStackingFailEvent event = BlockStackingFailEvent.of(joinCode, authenticatedPlayerName);
        streamPublisher.publish(GameStreamKey.BLOCKSTACKING_EVENTS, event);

        log.debug("블록 쌓기 실패 이벤트 발행: joinCode={}, playerName={}, eventId={}",
                joinCode, authenticatedPlayerName, event.eventId());
    }
}
