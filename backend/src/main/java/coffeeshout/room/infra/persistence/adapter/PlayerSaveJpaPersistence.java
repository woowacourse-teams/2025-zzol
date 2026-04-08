package coffeeshout.room.infra.persistence.adapter;

import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.repository.PlayerSavePersistence;
import coffeeshout.room.infra.persistence.PlayerEntity;
import coffeeshout.room.infra.persistence.PlayerJpaRepository;
import coffeeshout.room.infra.persistence.RoomEntity;
import coffeeshout.room.infra.persistence.RoomJpaRepository;
import coffeeshout.room.infra.persistence.mapper.PlayerEntityMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PlayerSaveJpaPersistence implements PlayerSavePersistence {

    private final RoomJpaRepository roomJpaRepository;
    private final PlayerJpaRepository playerJpaRepository;
    private final PlayerEntityMapper playerEntityMapper;

    @Override
    @Transactional
    public void saveAll(String joinCode, List<Player> players) {
        final RoomEntity roomEntity = roomJpaRepository.findFirstByJoinCodeOrderByCreatedAtDesc(joinCode)
                .orElseThrow(() -> new IllegalArgumentException("RoomEntity를 찾을 수 없습니다: " + joinCode));

        final List<PlayerEntity> playerEntities = playerEntityMapper.toEntities(players, roomEntity);
        playerEntities.forEach(playerJpaRepository::save);
    }
}
