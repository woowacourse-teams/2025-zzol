package coffeeshout.room.infra.persistence;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface NicknameFeedbackJpaRepository extends Repository<NicknameFeedbackEntity, Long> {

    NicknameFeedbackEntity save(NicknameFeedbackEntity entity);

    long count();

    // few-shot 주입용: 최근 N건을 created_at DESC 순으로 조회
    @Query("SELECT f FROM NicknameFeedbackEntity f ORDER BY f.createdAt DESC")
    List<NicknameFeedbackEntity> findRecentFeedbacks(Pageable pageable);
}
