package coffeeshout.laddergame.application;

import coffeeshout.websocket.LoggingSimpMessagingTemplate;
import coffeeshout.websocket.ui.WebSocketResponse;
import coffeeshout.laddergame.domain.LadderGame;
import coffeeshout.laddergame.domain.LadderLine;
import coffeeshout.laddergame.ui.response.LadderLineResponse;
import coffeeshout.laddergame.ui.response.LadderStateResponse;
import coffeeshout.room.domain.Room;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LadderNotifier {

    static final String STATE_DESTINATION_FORMAT = "/topic/room/%s/ladder/state";
    static final String LINE_DESTINATION_FORMAT = "/topic/room/%s/ladder/line";

    private final LoggingSimpMessagingTemplate messagingTemplate;

    public void notifyDescription(Room room) {
        sendState(room, LadderStateResponse.ofDescription());
    }

    public void notifyPrepare(LadderGame game, Room room) {
        sendState(room, LadderStateResponse.ofPrepare(game.getPoles(), game.getBottomRanks()));
    }

    public void notifyDrawing(Room room, Instant endTime) {
        sendState(room, LadderStateResponse.ofDrawing(endTime.toEpochMilli()));
    }

    public void notifyResult(LadderGame game, Room room, long animationDurationMs) {
        sendState(room, LadderStateResponse.ofResult(game.getRankingsForBroadcast(), animationDurationMs));
    }

    public void notifyDone(Room room) {
        sendState(room, LadderStateResponse.ofDone());
    }

    public void notifyLineDrawn(LadderLine line, Room room) {
        final String joinCode = room.getJoinCode().getValue();
        messagingTemplate.convertAndSend(
                String.format(LINE_DESTINATION_FORMAT, joinCode),
                WebSocketResponse.success(new LadderLineResponse(
                        line.playerName().value(),
                        line.segmentIndex(),
                        line.row(),
                        room.findPlayer(line.playerName()).getColorIndex()
                ))
        );
    }

    private void sendState(Room room, LadderStateResponse response) {
        final String joinCode = room.getJoinCode().getValue();
        messagingTemplate.convertAndSend(
                String.format(STATE_DESTINATION_FORMAT, joinCode),
                WebSocketResponse.success(response)
        );
    }
}
