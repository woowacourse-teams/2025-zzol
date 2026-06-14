package coffeeshout.minigame.infra.persistence;

import coffeeshout.minigame.application.port.MiniGameEntityRepository;
import org.springframework.data.repository.Repository;

public interface MiniGameJpaRepository extends Repository<MiniGameEntity, Long>, MiniGameEntityRepository {
}
