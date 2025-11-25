package coffeeshout.room.infra.persistence;

import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface PlayerJpaRepository extends Repository<PlayerEntity, Long> {
    PlayerEntity save(PlayerEntity playerEntity);

    Optional<PlayerEntity> findByRoomSessionAndPlayerName(RoomEntity roomSession, String playerName);
}
