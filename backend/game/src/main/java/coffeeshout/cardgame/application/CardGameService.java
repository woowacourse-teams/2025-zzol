package coffeeshout.cardgame.application;

import coffeeshout.cardgame.domain.CardGame;
import coffeeshout.cardgame.application.service.CardGameCommandService;
import coffeeshout.game.metric.GameDurationMetricService;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.domain.MiniGameService;
import coffeeshout.minigame.domain.MiniGameType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardGameService implements MiniGameService {

    private final GameSessionService gameSessionService;
    private final CardGameCommandService cardGameCommandService;
    private final CardGameFlowOrchestrator flowOrchestrator;
    private final GameDurationMetricService gameDurationMetricService;

    @Override
    public void start(String joinCode, String hostName) {
        final JoinCode code = new JoinCode(joinCode);
        final CardGame cardGame = getCardGame(code);
        flowOrchestrator.startFlow(cardGame, code);
        gameDurationMetricService.startGameTimer(joinCode);
    }

    public void selectCard(String joinCode, String playerName, Integer cardIndex) {
        final JoinCode code = new JoinCode(joinCode);
        final boolean roundFinished = cardGameCommandService.selectCard(code, playerName, cardIndex);
        if (roundFinished) {
            flowOrchestrator.triggerEarlyRoundFinish(joinCode);
        }
    }

    @Override
    public MiniGameType getMiniGameType() {
        return MiniGameType.CARD_GAME;
    }

    private CardGame getCardGame(JoinCode joinCode) {
        return (CardGame) gameSessionService.getSession(joinCode)
                .findCompletedGame(MiniGameType.CARD_GAME);
    }
}
