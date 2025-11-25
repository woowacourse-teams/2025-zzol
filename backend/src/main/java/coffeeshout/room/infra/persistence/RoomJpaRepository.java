package coffeeshout.room.infra.persistence;

import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface RoomJpaRepository extends Repository<RoomEntity, Long> {
    RoomEntity save(RoomEntity roomEntity);

    Optional<RoomEntity> findFirstByJoinCodeOrderByCreatedAtDesc(String joinCode);
}
