package coffeeshout.room.infra.persistence;

import coffeeshout.room.application.port.RouletteResultEntityRepository;
import org.springframework.data.repository.Repository;

public interface RouletteResultJpaRepository extends Repository<RouletteResultEntity, Long>, RouletteResultEntityRepository {
}
