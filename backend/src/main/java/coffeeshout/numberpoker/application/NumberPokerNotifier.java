package coffeeshout.numberpoker.application;

import coffeeshout.global.websocket.LoggingSimpMessagingTemplate;
import coffeeshout.global.websocket.PlayerKey;
import coffeeshout.global.websocket.ui.WebSocketResponse;
import coffeeshout.numberpoker.config.NumberPokerTimingProperties;
import coffeeshout.numberpoker.domain.NumberPokerGame;
import coffeeshout.numberpoker.domain.PokerPhase;
import coffeeshout.numberpoker.ui.response.PokerHandMessage;
import coffeeshout.numberpoker.ui.response.PokerStateMessage;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.Player;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NumberPokerNotifier {

    static final String POKER_STATE_TOPIC = "/topic/room/%s/poker/state";
    static final String POKER_HAND_QUEUE = "/queue/poker/hand";

    private final LoggingSimpMessagingTemplate messagingTemplate;
    private final NumberPokerTimingProperties timing;

    public void notifyPhaseChanged(NumberPokerGame game, Room room) {
        final Integer timerSeconds = timerSecondsOf(game.getCurrentPhase());
        messagingTemplate.convertAndSend(
                String.format(POKER_STATE_TOPIC, room.getJoinCode().getValue()),
                WebSocketResponse.success(PokerStateMessage.from(game, room, timerSeconds))
        );
    }

    public void notifyPhaseChanged(NumberPokerGame game, Room room, Map<Player, Integer> probabilityDeltas) {
        messagingTemplate.convertAndSend(
                String.format(POKER_STATE_TOPIC, room.getJoinCode().getValue()),
                WebSocketResponse.success(PokerStateMessage.from(game, room, probabilityDeltas))
        );
    }

    public void notifyHands(NumberPokerGame game, Room room) {
        for (Player player : room.getPlayers()) {
            final int[] cards = game.getPlayerCardValues(player);
            final String playerKey = PlayerKey.of(
                    room.getJoinCode().getValue(),
                    player.getName().value()
            ).toString();
            messagingTemplate.convertAndSendToUser(
                    playerKey,
                    POKER_HAND_QUEUE,
                    WebSocketResponse.success(new PokerHandMessage(cards[0], cards[1]))
            );
        }
    }

    /** STAGE_1·STAGE_2·ROUND_READY 에서만 타이머 초를 반환하고 나머지는 null */
    private Integer timerSecondsOf(PokerPhase phase) {
        if (phase == null) {
            return null;
        }
        return switch (phase) {
            case STAGE_1 -> (int) timing.stage1().toSeconds();
            case STAGE_2 -> (int) timing.stage2().toSeconds();
            case ROUND_READY -> (int) timing.roundReady().toSeconds();
            default -> null;
        };
    }
}
