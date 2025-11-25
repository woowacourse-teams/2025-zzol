package coffeeshout.cardgame.domain.event;

import coffeeshout.cardgame.domain.CardGame;
import coffeeshout.cardgame.domain.event.dto.CardGameStateChangedEvent;
import coffeeshout.cardgame.domain.event.dto.MiniGameStartedEvent;
import coffeeshout.global.ui.WebSocketResponse;
import coffeeshout.global.websocket.LoggingSimpMessagingTemplate;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.dto.CardSelectedEvent;
import coffeeshout.minigame.ui.response.MiniGameStartMessage;
import coffeeshout.minigame.ui.response.MiniGameStateMessage;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import generator.annotaions.MessageResponse;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/*
    TODO: redis도입시 event발송에서 redis pub/sub message 발송으로 변경하기
 */

@Component
public class CardGameMessagePublisher {

    private static final String CARD_GAME_STATE_DESTINATION_FORMAT = "/topic/room/%s/gameState";
    private static final String GAME_START_DESTINATION_FORMAT = "/topic/room/%s/round";

    private final LoggingSimpMessagingTemplate messagingTemplate;

    public CardGameMessagePublisher(LoggingSimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void publishCardGameStateChanged(CardGameStateChangedEvent cardGameStateChangedEvent) {
        final Room room = cardGameStateChangedEvent.room();
        final CardGame cardGame = cardGameStateChangedEvent.cardGame();
        sendCardGameState(cardGame, room.getJoinCode());
    }

    @EventListener
    @MessageResponse(
            path = "/room/{joinCode}/round",
            returnType = MiniGameStartMessage.class
    )
    public void publishCardGameStarted(MiniGameStartedEvent miniGameStartedEvent) {
        final MiniGameType miniGameType = MiniGameType.valueOf(miniGameStartedEvent.gameType());
        messagingTemplate.convertAndSend(
                String.format(GAME_START_DESTINATION_FORMAT, miniGameStartedEvent.joinCode()),
                WebSocketResponse.success(new MiniGameStartMessage(miniGameType))
        );
    }

    @EventListener
    public void publishCardSelected(CardSelectedEvent cardSelectedEvent) {
        sendCardGameState(cardSelectedEvent.cardGame(), cardSelectedEvent.joinCode());
    }

    private void sendCardGameState(CardGame cardGame, JoinCode joinCode) {
        final MiniGameStateMessage message = MiniGameStateMessage.from(cardGame);
        final String destination = String.format(CARD_GAME_STATE_DESTINATION_FORMAT, joinCode.getValue());
        messagingTemplate.convertAndSend(destination, WebSocketResponse.success(message));
    }
}
