package coffeeshout.minigame.application;

import coffeeshout.global.lock.RedisLock;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.domain.repository.MiniGamePersistence;
import coffeeshout.minigame.event.StartMiniGameCommandEvent;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.repository.PlayerSavePersistence;
import coffeeshout.room.domain.service.RoomQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MiniGamePersistenceService {

    private final RoomQueryService roomQueryService;
    private final MiniGamePersistence miniGamePersistence;
    private final PlayerSavePersistence playerSavePersistence;

    @RedisLock(
            key = "#event.eventId()",
            lockPrefix = "event:lock:",
            donePrefix = "event:done:",
            waitTime = 0,
            leaseTime = 5000
    )
    @Transactional
    public void saveGameEntities(StartMiniGameCommandEvent event, MiniGameType miniGameType) {
        final JoinCode roomJoinCode = new JoinCode(event.joinCode());
        final Room room = roomQueryService.getByJoinCode(roomJoinCode);

        miniGamePersistence.saveGameStart(event.joinCode(), miniGameType);

        if (room.isFirstStarted()) {
            playerSavePersistence.saveAll(event.joinCode(), room.getPlayers());
        }
    }
}
