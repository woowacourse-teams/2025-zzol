package coffeeshout.profanity.infra.persistence.audit;

import coffeeshout.profanity.application.port.NicknameFeedbackRepository;
import coffeeshout.profanity.domain.audit.NicknameFeedback;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface NicknameFeedbackJpaRepository extends Repository<NicknameFeedback, Long>, NicknameFeedbackRepository {

    @Override
    @Query("SELECT f FROM NicknameFeedback f ORDER BY f.createdAt DESC")
    List<NicknameFeedback> findRecentFeedbacks(Pageable pageable);
}
