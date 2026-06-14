package coffeeshout.minigame.application.port;

import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.infra.persistence.MiniGameEntity;
import coffeeshout.room.infra.persistence.RoomEntity;
import java.util.Optional;

public interface MiniGameEntityRepository {

    MiniGameEntity save(MiniGameEntity miniGameEntity);

    Optional<MiniGameEntity> findByRoomSessionAndMiniGameType(RoomEntity roomSession, MiniGameType miniGameType);
}
