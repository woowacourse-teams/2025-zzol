package coffeeshout.minigame.application.port;

import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.infra.persistence.MiniGameEntity;
import java.util.Optional;

public interface MiniGameEntityRepository {

    MiniGameEntity save(MiniGameEntity miniGameEntity);

    Optional<MiniGameEntity> findByRoomSessionIdAndMiniGameType(Long roomSessionId, MiniGameType miniGameType);
}
