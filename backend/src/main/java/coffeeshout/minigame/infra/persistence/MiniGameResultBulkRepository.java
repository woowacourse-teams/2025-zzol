package coffeeshout.minigame.infra.persistence;

import java.util.List;

public interface MiniGameResultBulkRepository {

    void bulkInsert(List<MiniGameResultEntity> resultEntities);
}
