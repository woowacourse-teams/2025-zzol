package coffeeshout.cardgame.application;

import coffeeshout.cardgame.domain.CardGame;
import coffeeshout.cardgame.domain.event.dto.MiniGameStartedEvent;
import coffeeshout.websocket.LoggingSimpMessagingTemplate;
import coffeeshout.websocket.ui.WebSocketResponse;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.ui.response.MiniGameStartMessage;
import coffeeshout.minigame.ui.response.MiniGameStateMessage;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import generator.annotaions.MessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CardGameNotifier {

    public static final String CARD_GAME_STATE_DESTINATION_FORMAT = "/topic/room/%s/gameState";
    public static final String GAME_START_DESTINATION_FORMAT = "/topic/room/%s/round";

    private final LoggingSimpMessagingTemplate messagingTemplate;

    public void notifyStepCompleted(CardGame cardGame, Room room) {
        sendGameState(cardGame, room.getJoinCode());
    }

    public void notifyCardSelected(JoinCode joinCode, CardGame cardGame) {
        sendGameState(cardGame, joinCode);
    }

    @EventListener
    @MessageResponse(
            path = "/room/{joinCode}/round",
            returnType = MiniGameStartMessage.class
    )
    public void notifyGameStarted(MiniGameStartedEvent event) {
        final MiniGameType miniGameType = MiniGameType.valueOf(event.gameType());
        messagingTemplate.convertAndSend(
                String.format(GAME_START_DESTINATION_FORMAT, event.joinCode()),
                WebSocketResponse.success(new MiniGameStartMessage(miniGameType))
        );
    }

    private void sendGameState(CardGame cardGame, JoinCode joinCode) {
        messagingTemplate.convertAndSend(
                String.format(CARD_GAME_STATE_DESTINATION_FORMAT, joinCode.getValue()),
                WebSocketResponse.success(MiniGameStateMessage.from(cardGame))
        );
    }
}
