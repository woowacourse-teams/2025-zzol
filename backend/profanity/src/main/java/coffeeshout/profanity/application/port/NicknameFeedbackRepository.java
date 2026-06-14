package coffeeshout.profanity.application.port;

import coffeeshout.profanity.domain.audit.NicknameFeedback;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface NicknameFeedbackRepository {

    NicknameFeedback save(NicknameFeedback entity);

    long count();

    List<NicknameFeedback> findRecentFeedbacks(Pageable pageable);
}
