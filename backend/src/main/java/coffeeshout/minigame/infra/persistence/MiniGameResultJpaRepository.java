package coffeeshout.minigame.infra.persistence;

import org.springframework.data.repository.Repository;

public interface MiniGameResultJpaRepository extends Repository<MiniGameResultEntity, Long>,
        MiniGameResultBulkRepository {

    MiniGameResultEntity save(MiniGameResultEntity miniGameResultEntity);
}
