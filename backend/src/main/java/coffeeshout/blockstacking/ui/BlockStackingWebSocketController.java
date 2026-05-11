package coffeeshout.blockstacking.ui;

import coffeeshout.blockstacking.domain.event.BlockStackingCommandEvent;
import coffeeshout.blockstacking.domain.event.BlockStackingFailEvent;
import coffeeshout.blockstacking.ui.request.BlockStackingProgressRequest;
import coffeeshout.blockstacking.infra.BlockStackingStreamKey;
import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.websocket.PlayerKey;
import generator.annotaions.MessageResponse;
import generator.annotaions.Operation;
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
    @Operation(
            summary = "블록 쌓기 진행 이벤트 전송",
            description = """
                    플레이어가 블록을 탭할 때마다 서버로 진행 정보를 전송하는 웹소켓 요청입니다.
                    서버는 overlap을 재계산하여 유효한 안착만 인정하고,
                    전체 플레이어의 랭킹을 /topic/room/{joinCode}/block-stacking/progress 로 브로드캐스트합니다.

                    유효하지 않은 이벤트(비연속 층수, overlap ≤ 0)는 서버에서 무시됩니다.
                    """
    )
    @MessageResponse(
            path = "/topic/room/{joinCode}/block-stacking/progress",
            returnType = Object.class
    )
    public void recordProgress(
            @DestinationVariable String joinCode,
            @Payload @Valid BlockStackingProgressRequest request,
            Principal principal
    ) {
        final String authenticatedPlayerName = PlayerKey.parse(principal.getName()).playerName();
        final BlockStackingCommandEvent event = BlockStackingCommandEvent.of(joinCode, authenticatedPlayerName, request);
        streamPublisher.publish(BlockStackingStreamKey.EVENTS, event);

        log.debug("블록 쌓기 진행 이벤트 발행: joinCode={}, playerName={}, floor={}, eventId = {}",
                joinCode, authenticatedPlayerName, request.floor(), event.eventId());
    }

    @MessageMapping("/room/{joinCode}/block-stacking/fail")
    @Operation(
            summary = "블록 쌓기 실패 이벤트 전송",
            description = """
                    플레이어가 블록을 쌓지 못하고 실패했을 때 서버로 전송하는 웹소켓 요청입니다.
                    모든 플레이어가 실패하면 남은 플레이 시간과 무관하게 2초 뒤 결과 화면으로 전환됩니다.
                    """
    )
    public void recordFail(
            @DestinationVariable String joinCode,
            Principal principal
    ) {
        final String authenticatedPlayerName = PlayerKey.parse(principal.getName()).playerName();
        final BlockStackingFailEvent event = BlockStackingFailEvent.of(joinCode, authenticatedPlayerName);
        streamPublisher.publish(BlockStackingStreamKey.EVENTS, event);

        log.debug("블록 쌓기 실패 이벤트 발행: joinCode={}, playerName={}, eventId={}",
                joinCode, authenticatedPlayerName, event.eventId());
    }
}
