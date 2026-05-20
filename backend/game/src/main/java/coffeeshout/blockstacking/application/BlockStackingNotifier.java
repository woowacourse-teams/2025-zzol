package coffeeshout.blockstacking.application;

import coffeeshout.blockstacking.domain.BlockStackingGame;
import coffeeshout.blockstacking.ui.response.BlockStackingProgressResponse;
import coffeeshout.blockstacking.ui.response.BlockStackingStateResponse;
import coffeeshout.room.domain.Room;
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
    public void notifyStateChanged(BlockStackingGame game, Room room) {
        final String joinCode = room.getJoinCode().getValue();
        messagingTemplate.convertAndSend(
                String.format(STATE_DESTINATION_FORMAT, joinCode),
                WebSocketResponse.success(BlockStackingStateResponse.of(game.getState()))
        );
    }

    @WsTopic(path = "/room/{joinCode}/block-stacking/state", payload = BlockStackingStateResponse.class,
            description = "블록 쌓기 플레이 시작 브로드캐스트")
    public void notifyPlayingStarted(Room room, Instant playingEndTime) {
        final String joinCode = room.getJoinCode().getValue();
        messagingTemplate.convertAndSend(
                String.format(STATE_DESTINATION_FORMAT, joinCode),
                WebSocketResponse.success(
                        BlockStackingStateResponse.ofPlaying(playingEndTime.toEpochMilli())
                )
        );
    }

    @WsTopic(path = "/room/{joinCode}/block-stacking/progress", payload = BlockStackingProgressResponse.class,
            description = "블록 쌓기 진행 상황 브로드캐스트")
    public void notifyProgressUpdated(BlockStackingGame game, Room room) {
        final String joinCode = room.getJoinCode().getValue();
        messagingTemplate.convertAndSend(
                String.format(PROGRESS_DESTINATION_FORMAT, joinCode),
                WebSocketResponse.success(new BlockStackingProgressResponse(game.getRanking()))
        );
    }
}
