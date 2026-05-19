package coffeeshout.room.infra.nickname.persistence;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface PlayerNameFeedbackJpaRepository extends Repository<PlayerNameFeedbackEntity, Long> {

    PlayerNameFeedbackEntity save(PlayerNameFeedbackEntity entity);

    long count();

    // few-shot 주입용: 최근 N건을 created_at DESC 순으로 조회
    @Query("SELECT f FROM PlayerNameFeedbackEntity f ORDER BY f.createdAt DESC")
    List<PlayerNameFeedbackEntity> findRecentFeedbacks(Pageable pageable);
}
