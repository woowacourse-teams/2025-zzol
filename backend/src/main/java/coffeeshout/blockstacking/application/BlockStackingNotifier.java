package coffeeshout.blockstacking.application;

import coffeeshout.blockstacking.domain.BlockStackingGame;
import coffeeshout.blockstacking.ui.response.BlockStackingProgressResponse;
import coffeeshout.blockstacking.ui.response.BlockStackingStateResponse;
import coffeeshout.global.websocket.LoggingSimpMessagingTemplate;
import coffeeshout.global.websocket.ui.WebSocketResponse;
import coffeeshout.room.domain.Room;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BlockStackingNotifier {

    static final String STATE_DESTINATION_FORMAT = "/topic/room/%s/block-stacking/state";
    static final String PROGRESS_DESTINATION_FORMAT = "/topic/room/%s/block-stacking/progress";

    private final LoggingSimpMessagingTemplate messagingTemplate;

    public void notifyStateChanged(BlockStackingGame game, Room room) {
        final String joinCode = room.getJoinCode().getValue();
        messagingTemplate.convertAndSend(
                String.format(STATE_DESTINATION_FORMAT, joinCode),
                WebSocketResponse.success(BlockStackingStateResponse.of(game.getState()))
        );
    }

    public void notifyPlayingStarted(Room room, Instant playingEndTime) {
        final String joinCode = room.getJoinCode().getValue();
        messagingTemplate.convertAndSend(
                String.format(STATE_DESTINATION_FORMAT, joinCode),
                WebSocketResponse.success(
                        BlockStackingStateResponse.ofPlaying(playingEndTime.toEpochMilli())
                )
        );
    }

    public void notifyProgressUpdated(BlockStackingGame game, Room room) {
        final String joinCode = room.getJoinCode().getValue();
        messagingTemplate.convertAndSend(
                String.format(PROGRESS_DESTINATION_FORMAT, joinCode),
                WebSocketResponse.success(new BlockStackingProgressResponse(game.getRanking()))
        );
    }
}
