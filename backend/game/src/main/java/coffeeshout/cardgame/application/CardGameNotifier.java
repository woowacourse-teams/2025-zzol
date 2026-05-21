package coffeeshout.cardgame.application;

import coffeeshout.cardgame.domain.CardGame;
import coffeeshout.minigame.domain.Gamer;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.MiniGameStartedEvent;
import coffeeshout.minigame.ui.response.MiniGameStartMessage;
import coffeeshout.minigame.ui.response.MiniGameStateMessage;
import coffeeshout.room.domain.JoinCode;
import java.util.Map;
import coffeeshout.websocket.LoggingSimpMessagingTemplate;
import coffeeshout.websocket.docs.WsTopic;
import coffeeshout.websocket.ui.WebSocketResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CardGameNotifier {

    public static final String CARD_GAME_STATE_DESTINATION_FORMAT = "/topic/room/%s/gameState";
    public static final String GAME_START_DESTINATION_FORMAT = "/topic/room/%s/round";

    private final LoggingSimpMessagingTemplate messagingTemplate;

    @WsTopic(path = "/room/{joinCode}/gameState", payload = MiniGameStateMessage.class,
            description = "카드게임 단계 완료 시 게임 상태 브로드캐스트")
    public void notifyStepCompleted(JoinCode joinCode, CardGame cardGame, Map<Gamer, Integer> colorMap) {
        sendGameState(cardGame, joinCode, colorMap);
    }

    @WsTopic(path = "/room/{joinCode}/gameState", payload = MiniGameStateMessage.class,
            description = "카드 선택 시 게임 상태 브로드캐스트")
    public void notifyCardSelected(JoinCode joinCode, CardGame cardGame, Map<Gamer, Integer> colorMap) {
        sendGameState(cardGame, joinCode, colorMap);
    }

    @EventListener
    @WsTopic(
            path = "/room/{joinCode}/round",
            payload = MiniGameStartMessage.class,
            description = "미니게임 시작 브로드캐스트"
    )
    public void notifyGameStarted(MiniGameStartedEvent event) {
        final MiniGameType miniGameType = MiniGameType.valueOf(event.gameType());
        messagingTemplate.convertAndSend(
                String.format(GAME_START_DESTINATION_FORMAT, event.joinCode()),
                WebSocketResponse.success(new MiniGameStartMessage(miniGameType))
        );
    }

    private void sendGameState(CardGame cardGame, JoinCode joinCode, Map<Gamer, Integer> colorMap) {
        messagingTemplate.convertAndSend(
                String.format(CARD_GAME_STATE_DESTINATION_FORMAT, joinCode.getValue()),
                WebSocketResponse.success(MiniGameStateMessage.from(cardGame, colorMap))
        );
    }
}
