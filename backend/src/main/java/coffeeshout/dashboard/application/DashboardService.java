package coffeeshout.dashboard.application;

import coffeeshout.dashboard.domain.GamePlayCountResponse;
import coffeeshout.dashboard.domain.LowestProbabilityWinnerResponse;
import coffeeshout.dashboard.domain.TopWinnerResponse;
import coffeeshout.dashboard.domain.repository.DashboardStatisticsRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardStatisticsRepository dashboardStatisticsRepository;

    public List<TopWinnerResponse> getTop5Winners() {
        final LocalDateTime startOfMonth = getStartOfMonth();
        final LocalDateTime endOfMonth = getEndOfMonth();

        return dashboardStatisticsRepository.findTopWinnersBetween(startOfMonth, endOfMonth, 5);
    }

    public LowestProbabilityWinnerResponse getLowestProbabilityWinner() {
        final LocalDateTime startOfMonth = getStartOfMonth();
        final LocalDateTime endOfMonth = getEndOfMonth();

        return dashboardStatisticsRepository.findLowestProbabilityWinner(startOfMonth, endOfMonth, 5)
                .orElse(null);
    }

    public List<GamePlayCountResponse> getGamePlayCounts() {
        final LocalDateTime startOfMonth = getStartOfMonth();
        final LocalDateTime endOfMonth = getEndOfMonth();

        return dashboardStatisticsRepository.findGamePlayCountByMonth(startOfMonth, endOfMonth);
    }

    private LocalDateTime getStartOfMonth() {
        return LocalDate.now().withDayOfMonth(1).atStartOfDay();
    }

    private LocalDateTime getEndOfMonth() {
        return getStartOfMonth().plusMonths(1).minusNanos(1);
    }
}
