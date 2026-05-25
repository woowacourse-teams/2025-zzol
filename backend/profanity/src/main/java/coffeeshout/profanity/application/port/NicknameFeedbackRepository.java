package coffeeshout.profanity.application.port;

import coffeeshout.profanity.infra.persistence.audit.NicknameFeedbackEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface NicknameFeedbackRepository {

    NicknameFeedbackEntity save(NicknameFeedbackEntity entity);

    long count();

    List<NicknameFeedbackEntity> findRecentFeedbacks(Pageable pageable);
}
