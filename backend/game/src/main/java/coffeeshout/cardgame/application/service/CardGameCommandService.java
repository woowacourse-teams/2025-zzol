package coffeeshout.cardgame.application.service;

import coffeeshout.cardgame.application.CardGameNotifier;
import coffeeshout.cardgame.domain.CardGame;
import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.domain.MiniGameType;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j

public class CardGameCommandService {

    private final GameSessionService gameSessionService;
    private final CardGameNotifier notifier;

    public boolean selectCard(JoinCode joinCode, String playerName, int cardIndex) {
        log.info("카드 선택 처리 시작: joinCode={}, playerName={}, cardIndex={}",
                joinCode, playerName, cardIndex);

        final CardGame cardGame = (CardGame) gameSessionService.getSession(joinCode)
                .findCompletedGame(MiniGameType.CARD_GAME);
        final Gamer gamer = cardGame.findByName(playerName);
        final boolean roundFinished = cardGame.selectCard(gamer, cardIndex);

        notifier.notifyCardSelected(joinCode, cardGame);
        return roundFinished;
    }
}
