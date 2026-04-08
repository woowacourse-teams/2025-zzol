package coffeeshout.minigame.event;

import coffeeshout.global.lock.RedisLock;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.domain.repository.MiniGameResultSavePersistence;
import coffeeshout.minigame.event.dto.MiniGameFinishedEvent;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Playable;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.service.RoomQueryService;
import jakarta.transaction.Transactional;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MiniGameResultSaveEventListener {

    private final MiniGameResultSavePersistence miniGameResultSavePersistence;
    private final RoomQueryService roomQueryService;

    @EventListener
    @Transactional
    @RedisLock(
            key = "#event.eventId()",
            lockPrefix = "minigame:result:lock:",
            donePrefix = "minigame:result:done:",
            waitTime = 0,
            leaseTime = 5000
    )
    public void handle(MiniGameFinishedEvent event) {
        final MiniGameType miniGameType = MiniGameType.valueOf(event.miniGameType());
        final Room room = roomQueryService.getByJoinCode(new JoinCode(event.joinCode()));
        final Playable miniGame = room.findMiniGame(miniGameType);

        final MiniGameResult result = miniGame.getResult();
        final Map<Player, MiniGameScore> scores = miniGame.getScores();

        miniGameResultSavePersistence.saveResults(event.joinCode(), miniGameType, room.getPlayers(), result, scores);

        log.info("미니게임 결과 저장 완료: joinCode={}, miniGameType={}", event.joinCode(), miniGameType);
    }
}
