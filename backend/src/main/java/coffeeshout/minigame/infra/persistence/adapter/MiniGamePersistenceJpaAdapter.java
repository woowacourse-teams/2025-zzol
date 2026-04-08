package coffeeshout.minigame.infra.persistence.adapter;

import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.domain.repository.MiniGamePersistencePort;
import coffeeshout.minigame.infra.persistence.MiniGameEntity;
import coffeeshout.minigame.infra.persistence.MiniGameJpaRepository;
import coffeeshout.room.domain.RoomState;
import coffeeshout.room.infra.persistence.RoomEntity;
import coffeeshout.room.infra.persistence.RoomJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MiniGamePersistenceJpaAdapter implements MiniGamePersistencePort {

    private final RoomJpaRepository roomJpaRepository;
    private final MiniGameJpaRepository miniGameJpaRepository;

    @Override
    @Transactional
    public void saveGameStart(String joinCode, MiniGameType miniGameType) {
        final RoomEntity roomEntity = roomJpaRepository.findFirstByJoinCodeOrderByCreatedAtDesc(joinCode)
                .orElseThrow(() -> new IllegalArgumentException("방이 존재하지 않습니다: " + joinCode));

        roomEntity.updateRoomStatus(RoomState.PLAYING);
        miniGameJpaRepository.save(new MiniGameEntity(roomEntity, miniGameType));
    }
}
