package coffeeshout.cardgame.application;

import coffeeshout.cardgame.domain.CardGame;
import coffeeshout.cardgame.domain.CardGameTaskType;
import coffeeshout.cardgame.domain.service.CardGameCommandService;
import coffeeshout.global.metric.GameDurationMetricService;
import coffeeshout.minigame.domain.MiniGameService;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.service.RoomQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CardGameService implements MiniGameService {

    private final RoomQueryService roomQueryService;
    private final CardGameCommandService cardGameCommandService;
    private final TaskScheduler taskScheduler;
    private final GameDurationMetricService gameDurationMetricService;
    private final ApplicationEventPublisher eventPublisher;

    public CardGameService(
            RoomQueryService roomQueryService,
            CardGameCommandService cardGameCommandService,
            @Qualifier("cardGameTaskScheduler") TaskScheduler taskScheduler,
            GameDurationMetricService gameDurationMetricService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.roomQueryService = roomQueryService;
        this.cardGameCommandService = cardGameCommandService;
        this.taskScheduler = taskScheduler;
        this.gameDurationMetricService = gameDurationMetricService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void start(String joinCode, String hostName) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        final CardGame cardGame = getCardGame(room);
        CardGameTaskType.getFirstTask().processTask(cardGame, room, taskScheduler, eventPublisher);
        gameDurationMetricService.startGameTimer(joinCode);
    }

    public void selectCard(String joinCode, String playerName, Integer cardIndex) {
        cardGameCommandService.selectCard(new JoinCode(joinCode), new PlayerName(playerName), cardIndex);
    }

    @Override
    public MiniGameType getMiniGameType() {
        return MiniGameType.CARD_GAME;
    }

    private CardGame getCardGame(Room room) {
        return (CardGame) room.findMiniGame(MiniGameType.CARD_GAME);
    }
}
