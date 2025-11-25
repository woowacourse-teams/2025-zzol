package coffeeshout.minigame.infra.persistence;

import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.infra.persistence.RoomEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface MiniGameJpaRepository extends Repository<MiniGameEntity, Long> {

    MiniGameEntity save(MiniGameEntity miniGameEntity);

    List<MiniGameEntity> findByRoomSession(RoomEntity roomSession);

    Optional<MiniGameEntity> findByRoomSessionAndMiniGameType(RoomEntity roomSession, MiniGameType miniGameType);
}
