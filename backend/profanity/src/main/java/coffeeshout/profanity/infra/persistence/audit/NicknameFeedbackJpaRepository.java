package coffeeshout.profanity.infra.persistence.audit;

import coffeeshout.profanity.application.port.NicknameFeedbackRepository;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface NicknameFeedbackJpaRepository extends Repository<NicknameFeedbackEntity, Long>, NicknameFeedbackRepository {

    @Override
    @Query("SELECT f FROM NicknameFeedbackEntity f ORDER BY f.createdAt DESC")
    List<NicknameFeedbackEntity> findRecentFeedbacks(Pageable pageable);
}
