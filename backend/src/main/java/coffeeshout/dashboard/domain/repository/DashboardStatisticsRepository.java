package coffeeshout.dashboard.domain.repository;

import coffeeshout.dashboard.domain.GamePlayCountResponse;
import coffeeshout.dashboard.domain.LowestProbabilityWinnerResponse;
import coffeeshout.dashboard.domain.TopWinnerResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DashboardStatisticsRepository {

    List<TopWinnerResponse> findTopWinnersBetween(
            LocalDateTime startDate,
            LocalDateTime endDate,
            int limit
    );

    Optional<LowestProbabilityWinnerResponse> findLowestProbabilityWinner(
            LocalDateTime startDate,
            LocalDateTime endDate,
            int limit
    );

    List<GamePlayCountResponse> findGamePlayCountByMonth(LocalDateTime startDate, LocalDateTime endDate);
}
