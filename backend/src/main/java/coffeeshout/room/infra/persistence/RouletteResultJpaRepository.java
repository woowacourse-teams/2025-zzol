package coffeeshout.room.infra.persistence;

import org.springframework.data.repository.Repository;

public interface RouletteResultJpaRepository extends Repository<RouletteResultEntity, Long> {

    RouletteResultEntity save(RouletteResultEntity entity);
}
