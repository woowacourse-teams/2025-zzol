package coffeeshout.room.application.port;

import coffeeshout.room.infra.persistence.nickname.PlayerNameFeedbackEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface PlayerNameFeedbackRepository {

    PlayerNameFeedbackEntity save(PlayerNameFeedbackEntity entity);

    long count();

    List<PlayerNameFeedbackEntity> findRecentFeedbacks(Pageable pageable);
}
