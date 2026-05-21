package coffeeshout.cardgame.application;

import coffeeshout.cardgame.domain.CardGame;
import coffeeshout.cardgame.domain.service.CardGameCommandService;
import coffeeshout.gamecommon.metric.GameDurationMetricService;
import coffeeshout.minigame.domain.MiniGameService;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.service.RoomQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardGameService implements MiniGameService {

    private final RoomQueryService roomQueryService;
    private final CardGameCommandService cardGameCommandService;
    private final CardGameFlowOrchestrator flowOrchestrator;
    private final GameDurationMetricService gameDurationMetricService;

    @Override
    public void start(String joinCode, String hostName) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        final CardGame cardGame = getCardGame(room);
        flowOrchestrator.startFlow(cardGame, room);
        gameDurationMetricService.startGameTimer(joinCode);
    }

    public void selectCard(String joinCode, String playerName, Integer cardIndex) {
        final JoinCode code = new JoinCode(joinCode);
        final boolean roundFinished = cardGameCommandService.selectCard(code, new PlayerName(playerName), cardIndex);
        if (roundFinished) {
            flowOrchestrator.triggerEarlyRoundFinish(joinCode);
        }
    }

    @Override
    public MiniGameType getMiniGameType() {
        return MiniGameType.CARD_GAME;
    }

    private CardGame getCardGame(Room room) {
        return (CardGame) room.findMiniGame(MiniGameType.CARD_GAME);
    }
}
