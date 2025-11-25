package coffeeshout.minigame.application;

import coffeeshout.global.lock.RedisLock;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.StartMiniGameCommandEvent;
import coffeeshout.minigame.infra.persistence.MiniGameEntity;
import coffeeshout.minigame.infra.persistence.MiniGameJpaRepository;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.RoomState;
import coffeeshout.room.domain.service.RoomQueryService;
import coffeeshout.room.infra.persistence.PlayerEntity;
import coffeeshout.room.infra.persistence.PlayerJpaRepository;
import coffeeshout.room.infra.persistence.RoomEntity;
import coffeeshout.room.infra.persistence.RoomJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MiniGamePersistenceService {

    private final RoomQueryService roomQueryService;
    private final RoomJpaRepository roomJpaRepository;
    private final PlayerJpaRepository playerJpaRepository;
    private final MiniGameJpaRepository miniGameJpaRepository;

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

        final RoomEntity roomEntity = getRoomEntity(event.joinCode());
        roomEntity.updateRoomStatus(RoomState.PLAYING);

        final MiniGameEntity miniGameEntity = new MiniGameEntity(roomEntity, miniGameType);
        miniGameJpaRepository.save(miniGameEntity);

        if (room.isFirstStarted()) {
            room.getPlayers().forEach(player -> {
                final PlayerEntity playerEntity = new PlayerEntity(
                        roomEntity,
                        player.getName().value(),
                        player.getPlayerType()
                );
                playerJpaRepository.save(playerEntity);
            });
        }
    }

    private RoomEntity getRoomEntity(String joinCode) {
        return roomJpaRepository.findFirstByJoinCodeOrderByCreatedAtDesc(joinCode)
                .orElseThrow(() -> new IllegalArgumentException("방이 존재하지 않습니다: " + joinCode));
    }
}
