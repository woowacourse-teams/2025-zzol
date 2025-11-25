package coffeeshout.cardgame.domain.service;


import coffeeshout.cardgame.domain.CardGame;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.dto.CardSelectedEvent;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.service.RoomQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardGameCommandService {

    private final RoomQueryService roomQueryService;
    private final ApplicationEventPublisher eventPublisher;

    public void selectCard(JoinCode joinCode, PlayerName playerName, int cardIndex) {
        log.info("카드 선택 처리 시작: joinCode={}, playerName={}, cardIndex={}",
                joinCode, playerName, cardIndex);

        final CardGame cardGame = getCardGame(joinCode);
        final Player player = cardGame.findPlayerByName(playerName);
        cardGame.selectCard(player, cardIndex);

        eventPublisher.publishEvent(new CardSelectedEvent(joinCode, cardGame));
    }

    private CardGame getCardGame(JoinCode joinCode) {
        final Room room = roomQueryService.getByJoinCode(joinCode);
        return (CardGame) room.findMiniGame(MiniGameType.CARD_GAME);
    }
}
