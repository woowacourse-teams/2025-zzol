package coffeeshout.blockstacking.ui;

import coffeeshout.blockstacking.application.BlockStackingService;
import coffeeshout.blockstacking.ui.request.BlockStackingProgressRequest;
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
public class BlockStackingWebSocketController {

    private final BlockStackingService blockStackingService;

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
            @Payload @Valid BlockStackingProgressRequest request
    ) {
        blockStackingService.recordProgress(
                joinCode,
                request.playerName(),
                request.floor(),
                request.tapX(),
                request.movingBlockX(),
                request.stackTopX(),
                request.stackTopWidth()
        );
    }
}
