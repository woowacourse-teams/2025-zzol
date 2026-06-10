package coffeeshout.laddergame.application;

import coffeeshout.gamecommon.JoinCode;
import coffeeshout.websocket.LoggingSimpMessagingTemplate;
import coffeeshout.websocket.docs.WsTopic;
import coffeeshout.websocket.ui.WebSocketResponse;
import coffeeshout.laddergame.domain.LadderGame;
import coffeeshout.laddergame.domain.LadderLine;
import coffeeshout.laddergame.application.response.LadderLineResponse;
import coffeeshout.laddergame.application.response.LadderStateResponse;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LadderNotifier {

    static final String STATE_DESTINATION_FORMAT = "/topic/room/%s/ladder/state";
    static final String LINE_DESTINATION_FORMAT = "/topic/room/%s/ladder/line";

    private final LoggingSimpMessagingTemplate messagingTemplate;

    @WsTopic(path = "/room/{joinCode}/ladder/state", payload = LadderStateResponse.class,
            description = "사다리게임 설명 상태 브로드캐스트")
    public void notifyDescription(JoinCode joinCode) {
        sendState(joinCode, LadderStateResponse.ofDescription());
    }

    @WsTopic(path = "/room/{joinCode}/ladder/state", payload = LadderStateResponse.class,
            description = "사다리게임 준비 상태 브로드캐스트")
    public void notifyPrepare(LadderGame game, JoinCode joinCode) {
        sendState(joinCode, LadderStateResponse.ofPrepare(game.getPoles(), game.getBottomRanks()));
    }

    @WsTopic(path = "/room/{joinCode}/ladder/state", payload = LadderStateResponse.class,
            description = "사다리게임 그리기 상태 브로드캐스트")
    public void notifyDrawing(JoinCode joinCode, Instant endTime) {
        sendState(joinCode, LadderStateResponse.ofDrawing(endTime.toEpochMilli()));
    }

    @WsTopic(path = "/room/{joinCode}/ladder/state", payload = LadderStateResponse.class,
            description = "사다리게임 결과 브로드캐스트")
    public void notifyResult(LadderGame game, JoinCode joinCode, long animationDurationMs) {
        sendState(joinCode, LadderStateResponse.ofResult(game.getRankingsForBroadcast(), animationDurationMs));
    }

    @WsTopic(path = "/room/{joinCode}/ladder/state", payload = LadderStateResponse.class,
            description = "사다리게임 완료 브로드캐스트")
    public void notifyDone(JoinCode joinCode) {
        sendState(joinCode, LadderStateResponse.ofDone());
    }

    @WsTopic(path = "/room/{joinCode}/ladder/line", payload = LadderLineResponse.class,
            description = "사다리 선 그리기 브로드캐스트")
    public void notifyLineDrawn(LadderGame game, LadderLine line, JoinCode joinCode) {
        messagingTemplate.convertAndSend(
                String.format(LINE_DESTINATION_FORMAT, joinCode.getValue()),
                WebSocketResponse.success(new LadderLineResponse(
                        line.playerName(),
                        line.segmentIndex(),
                        line.row(),
                        game.getPoles().findGamer(line.playerName()).getColorIndex()
                ))
        );
    }

    private void sendState(JoinCode joinCode, LadderStateResponse response) {
        messagingTemplate.convertAndSend(
                String.format(STATE_DESTINATION_FORMAT, joinCode.getValue()),
                WebSocketResponse.success(response)
        );
    }
}
