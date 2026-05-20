package coffeeshout.cardgame.domain.service;

import coffeeshout.cardgame.application.CardGameNotifier;
import coffeeshout.cardgame.domain.CardGame;
import coffeeshout.minigame.domain.GameSessionRepository;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.player.PlayerName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardGameCommandService {

    private final GameSessionRepository gameSessionRepository;
    private final CardGameNotifier notifier;

    public boolean selectCard(JoinCode joinCode, PlayerName playerName, int cardIndex) {
        log.info("카드 선택 처리 시작: joinCode={}, playerName={}, cardIndex={}",
                joinCode, playerName, cardIndex);

        final CardGame cardGame = (CardGame) gameSessionRepository.getByJoinCode(joinCode)
                .findCompletedGame(MiniGameType.CARD_GAME);
        final boolean roundFinished = cardGame.selectCard(playerName, cardIndex);

        notifier.notifyCardSelected(joinCode, cardGame);
        return roundFinished;
    }
}
