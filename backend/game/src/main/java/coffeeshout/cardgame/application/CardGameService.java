package coffeeshout.cardgame.application;

import coffeeshout.cardgame.domain.CardGame;
import coffeeshout.gamecommon.metric.GameDurationMetricService;
import coffeeshout.minigame.domain.GameSessionRepository;
import coffeeshout.minigame.domain.MiniGameService;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.minigame.domain.Gamer;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.service.RoomQueryService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardGameService implements MiniGameService {

    private final RoomQueryService roomQueryService;
    private final GameSessionRepository gameSessionRepository;
    private final CardGameFlowOrchestrator flowOrchestrator;
    private final CardGameNotifier notifier;
    private final GameDurationMetricService gameDurationMetricService;

    @Override
    public void start(String joinCode, String hostName) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        final CardGame cardGame = getCardGame(joinCode);
        flowOrchestrator.startFlow(cardGame, room);
        gameDurationMetricService.startGameTimer(joinCode);
    }

    public void selectCard(String joinCode, String playerName, Long userId, Integer cardIndex) {
        log.info("카드 선택 처리 시작: joinCode={}, playerName={}, cardIndex={}", joinCode, playerName, cardIndex);

        final JoinCode code = new JoinCode(joinCode);
        final CardGame cardGame = getCardGame(joinCode);
        final Gamer gamer = Gamer.of(playerName, userId);
        final boolean roundFinished = cardGame.selectCard(gamer, cardIndex);

        final Map<PlayerName, Integer> colorMap = roomQueryService.getColorIndexMap(code);
        notifier.notifyCardSelected(code, cardGame, colorMap);

        if (roundFinished) {
            flowOrchestrator.triggerEarlyRoundFinish(joinCode);
        }
    }

    @Override
    public MiniGameType getMiniGameType() {
        return MiniGameType.CARD_GAME;
    }

    private CardGame getCardGame(String joinCode) {
        return (CardGame) gameSessionRepository.getByJoinCode(new JoinCode(joinCode))
                .findCompletedGame(MiniGameType.CARD_GAME);
    }
}
