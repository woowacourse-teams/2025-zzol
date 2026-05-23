package coffeeshout.room.application.port;

import coffeeshout.room.infra.persistence.RoomEntity;
import java.util.Optional;

public interface RoomEntityRepository {

    RoomEntity save(RoomEntity roomEntity);

    RoomEntity saveAndFlush(RoomEntity roomEntity);

    Optional<RoomEntity> findFirstByJoinCodeOrderByCreatedAtDesc(String joinCode);
}
