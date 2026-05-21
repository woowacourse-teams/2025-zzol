package coffeeshout.cardgame.domain.service;

import coffeeshout.cardgame.application.CardGameNotifier;
import coffeeshout.cardgame.domain.CardGame;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.service.RoomQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardGameCommandService {

    private final RoomQueryService roomQueryService;
    private final CardGameNotifier notifier;

    public boolean selectCard(JoinCode joinCode, PlayerName playerName, int cardIndex) {
        log.info("카드 선택 처리 시작: joinCode={}, playerName={}, cardIndex={}",
                joinCode, playerName, cardIndex);

        final Room room = roomQueryService.getByJoinCode(joinCode);
        final CardGame cardGame = (CardGame) room.findMiniGame(MiniGameType.CARD_GAME);
        final Player player = cardGame.findPlayerByName(playerName);
        final boolean roundFinished = cardGame.selectCard(player, cardIndex);

        notifier.notifyCardSelected(joinCode, cardGame);
        return roundFinished;
    }
}
