package coffeeshout.room.infra.persistence;

import coffeeshout.room.application.port.RoomEntityRepository;
import org.springframework.data.repository.Repository;

public interface RoomJpaRepository extends Repository<RoomEntity, Long>, RoomEntityRepository {
}
