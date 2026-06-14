package coffeeshout.blockstacking.application;

import coffeeshout.blockstacking.domain.BlockStackingGame;
import coffeeshout.blockstacking.application.response.BlockStackingProgressResponse;
import coffeeshout.blockstacking.application.response.BlockStackingStateResponse;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.websocket.LoggingSimpMessagingTemplate;
import coffeeshout.websocket.docs.WsTopic;
import coffeeshout.websocket.ui.WebSocketResponse;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BlockStackingNotifier {

    static final String STATE_DESTINATION_FORMAT = "/topic/room/%s/block-stacking/state";
    static final String PROGRESS_DESTINATION_FORMAT = "/topic/room/%s/block-stacking/progress";

    private final LoggingSimpMessagingTemplate messagingTemplate;

    @WsTopic(path = "/room/{joinCode}/block-stacking/state", payload = BlockStackingStateResponse.class,
            description = "블록 쌓기 상태 변경 브로드캐스트")
    public void notifyStateChanged(BlockStackingGame game, JoinCode joinCode) {
        messagingTemplate.convertAndSend(
                String.format(STATE_DESTINATION_FORMAT, joinCode.getValue()),
                WebSocketResponse.success(BlockStackingStateResponse.of(game.getState()))
        );
    }

    @WsTopic(path = "/room/{joinCode}/block-stacking/state", payload = BlockStackingStateResponse.class,
            description = "블록 쌓기 플레이 시작 브로드캐스트")
    public void notifyPlayingStarted(JoinCode joinCode, Instant playingEndTime) {
        messagingTemplate.convertAndSend(
                String.format(STATE_DESTINATION_FORMAT, joinCode.getValue()),
                WebSocketResponse.success(
                        BlockStackingStateResponse.ofPlaying(playingEndTime.toEpochMilli())
                )
        );
    }

    @WsTopic(path = "/room/{joinCode}/block-stacking/progress", payload = BlockStackingProgressResponse.class,
            description = "블록 쌓기 진행 상황 브로드캐스트")
    public void notifyProgressUpdated(BlockStackingGame game, JoinCode joinCode) {
        messagingTemplate.convertAndSend(
                String.format(PROGRESS_DESTINATION_FORMAT, joinCode.getValue()),
                WebSocketResponse.success(new BlockStackingProgressResponse(game.getRanking()))
        );
    }
}
