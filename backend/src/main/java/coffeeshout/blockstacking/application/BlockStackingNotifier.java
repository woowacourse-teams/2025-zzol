package coffeeshout.blockstacking.application;

import coffeeshout.blockstacking.domain.BlockStackingGame;
import coffeeshout.blockstacking.domain.BlockStackingPlayerRankInfo;
import coffeeshout.global.websocket.LoggingSimpMessagingTemplate;
import coffeeshout.global.websocket.ui.WebSocketResponse;
import coffeeshout.room.domain.Room;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BlockStackingNotifier {

    static final String STATE_DESTINATION_FORMAT = "/topic/room/%s/block-stacking/state";
    static final String PROGRESS_DESTINATION_FORMAT = "/topic/room/%s/block-stacking/progress";
    static final String COMPLETE_DESTINATION_FORMAT = "/topic/room/%s/block-stacking/complete";

    private final LoggingSimpMessagingTemplate messagingTemplate;

    public void notifyStateChanged(BlockStackingGame game, Room room) {
        final String joinCode = room.getJoinCode().getValue();
        messagingTemplate.convertAndSend(
                String.format(STATE_DESTINATION_FORMAT, joinCode),
                WebSocketResponse.success(new BlockStackingStateMessage(game.getState().name()))
        );
    }

    public void notifyProgressUpdated(BlockStackingGame game, Room room) {
        final String joinCode = room.getJoinCode().getValue();
        final List<BlockStackingPlayerRankInfo> ranking = game.getRanking();
        messagingTemplate.convertAndSend(
                String.format(PROGRESS_DESTINATION_FORMAT, joinCode),
                WebSocketResponse.success(new BlockStackingProgressMessage(ranking))
        );
    }

    public void notifyGameComplete(Room room) {
        final String joinCode = room.getJoinCode().getValue();
        messagingTemplate.convertAndSend(
                String.format(COMPLETE_DESTINATION_FORMAT, joinCode),
                WebSocketResponse.success(new BlockStackingCompleteMessage("DONE"))
        );
    }

    public record BlockStackingStateMessage(String state) {
    }

    public record BlockStackingProgressMessage(List<BlockStackingPlayerRankInfo> players) {
    }

    public record BlockStackingCompleteMessage(String state) {
    }
}
