package coffeeshout.cardgame.application.service;

import coffeeshout.cardgame.application.CardGameNotifier;
import coffeeshout.cardgame.domain.CardGame;
import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.domain.Room;
import coffeeshout.room.application.service.RoomQueryService;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j

public class CardGameCommandService {

    private final RoomQueryService roomQueryService;
    private final CardGameNotifier notifier;

    public boolean selectCard(JoinCode joinCode, String playerName, int cardIndex) {
        log.info("카드 선택 처리 시작: joinCode={}, playerName={}, cardIndex={}",
                joinCode, playerName, cardIndex);

        final Room room = roomQueryService.getByJoinCode(joinCode);
        final CardGame cardGame = (CardGame) room.findMiniGame(MiniGameType.CARD_GAME);
        final Gamer gamer = cardGame.findByName(playerName);
        final boolean roundFinished = cardGame.selectCard(gamer, cardIndex);

        notifier.notifyCardSelected(joinCode, cardGame);
        return roundFinished;
    }
}
