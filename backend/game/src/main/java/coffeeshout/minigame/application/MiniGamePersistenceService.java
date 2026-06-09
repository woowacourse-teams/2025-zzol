package coffeeshout.minigame.application;

import coffeeshout.gamecommon.JoinCode;
import coffeeshout.global.lock.RedisLock;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.StartMiniGameCommandEvent;
import coffeeshout.minigame.application.port.MiniGameEntityRepository;
import coffeeshout.minigame.infra.persistence.MiniGameEntity;
import coffeeshout.room.application.port.PlayerEntityRepository;
import coffeeshout.room.application.port.RoomEntityRepository;
import coffeeshout.room.application.port.RoomStatusPort;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.RoomState;
import coffeeshout.room.application.service.RoomQueryService;
import coffeeshout.room.infra.persistence.PlayerEntity;
import coffeeshout.room.infra.persistence.RoomEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MiniGamePersistenceService {

    private final RoomQueryService roomQueryService;
    private final RoomEntityRepository roomEntityRepository;
    private final PlayerEntityRepository playerEntityRepository;
    private final MiniGameEntityRepository miniGameEntityRepository;
    private final RoomStatusPort roomStatusPort;

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

        // getRoomEntity()로 먼저 엔티티를 가져와 오버로드에 넘김 — 조회 1회로 통합
        final RoomEntity roomEntity = getRoomEntity(event.joinCode());
        roomStatusPort.updateStatus(roomEntity, RoomState.PLAYING);
        final MiniGameEntity miniGameEntity = new MiniGameEntity(roomEntity, miniGameType);
        miniGameEntityRepository.save(miniGameEntity);

        if (room.isFirstStarted()) {
            room.getPlayers().forEach(player -> {
                final PlayerEntity playerEntity = new PlayerEntity(
                        roomEntity,
                        player.getName().value(),
                        player.getPlayerType(),
                        player.getUserId()
                );
                playerEntityRepository.save(playerEntity);
            });
        }
    }

    private RoomEntity getRoomEntity(String joinCode) {
        return roomEntityRepository.findFirstByJoinCodeOrderByCreatedAtDesc(joinCode)
                .orElseThrow(() -> new IllegalArgumentException("방이 존재하지 않습니다: " + joinCode));
    }
}
