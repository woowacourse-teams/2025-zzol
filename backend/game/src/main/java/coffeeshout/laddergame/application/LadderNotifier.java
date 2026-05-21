package coffeeshout.laddergame.application;

import coffeeshout.laddergame.domain.LadderGame;
import coffeeshout.laddergame.domain.LadderLine;
import coffeeshout.laddergame.ui.response.LadderLineResponse;
import coffeeshout.laddergame.ui.response.LadderStateResponse;
import coffeeshout.minigame.domain.Gamer;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.Player;
import java.util.Map;
import java.util.stream.Collectors;
import coffeeshout.websocket.LoggingSimpMessagingTemplate;
import coffeeshout.websocket.docs.WsTopic;
import coffeeshout.websocket.ui.WebSocketResponse;
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
    public void notifyDescription(Room room) {
        sendState(room, LadderStateResponse.ofDescription());
    }

    @WsTopic(path = "/room/{joinCode}/ladder/state", payload = LadderStateResponse.class,
            description = "사다리게임 준비 상태 브로드캐스트")
    public void notifyPrepare(LadderGame game, Room room) {
        sendState(room, LadderStateResponse.ofPrepare(game.getPoles(), game.getBottomRanks(), buildGamerColorMap(room)));
    }

    @WsTopic(path = "/room/{joinCode}/ladder/state", payload = LadderStateResponse.class,
            description = "사다리게임 그리기 상태 브로드캐스트")
    public void notifyDrawing(Room room, Instant endTime) {
        sendState(room, LadderStateResponse.ofDrawing(endTime.toEpochMilli()));
    }

    @WsTopic(path = "/room/{joinCode}/ladder/state", payload = LadderStateResponse.class,
            description = "사다리게임 결과 브로드캐스트")
    public void notifyResult(LadderGame game, Room room, long animationDurationMs) {
        sendState(room, LadderStateResponse.ofResult(game.getRankingsForBroadcast(), animationDurationMs));
    }

    @WsTopic(path = "/room/{joinCode}/ladder/state", payload = LadderStateResponse.class,
            description = "사다리게임 완료 브로드캐스트")
    public void notifyDone(Room room) {
        sendState(room, LadderStateResponse.ofDone());
    }

    @WsTopic(path = "/room/{joinCode}/ladder/line", payload = LadderLineResponse.class,
            description = "사다리 선 그리기 브로드캐스트")
    public void notifyLineDrawn(LadderLine line, JoinCode joinCode, Map<Gamer, Integer> colorMap) {
        messagingTemplate.convertAndSend(
                String.format(LINE_DESTINATION_FORMAT, joinCode.getValue()),
                WebSocketResponse.success(new LadderLineResponse(
                        line.gamer().name().value(),
                        line.segmentIndex(),
                        line.row(),
                        colorMap.get(line.gamer())
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

    private static Map<Gamer, Integer> buildGamerColorMap(Room room) {
        return room.getPlayers().stream()
                .collect(Collectors.toUnmodifiableMap(
                        player -> player.getUserId() != null
                                  ? Gamer.loggedIn(player.getName(), player.getUserId())
                                  : Gamer.guest(player.getName()),
                        Player::getColorIndex
                ));
    }
}
