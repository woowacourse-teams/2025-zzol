package coffeeshout.dashboard.application;

import coffeeshout.dashboard.domain.BlockStackingTopPlayerResponse;
import coffeeshout.dashboard.domain.GamePlayCountResponse;
import coffeeshout.dashboard.domain.LowestProbabilityWinnerResponse;
import coffeeshout.dashboard.domain.RacingGameTopPlayerResponse;
import coffeeshout.dashboard.domain.TopWinnerResponse;
import coffeeshout.dashboard.domain.repository.DashboardStatisticsRepository;
import java.time.Clock;
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

    private static final int TOP_PLAYER_LIMIT = 5;

    private final DashboardStatisticsRepository dashboardStatisticsRepository;
    private final Clock clock;

    public List<TopWinnerResponse> getTop5Winners() {
        final LocalDateTime startOfMonth = getStartOfMonth();
        final LocalDateTime endOfMonth = getEndOfMonth();

        return dashboardStatisticsRepository.findTopWinnersBetween(startOfMonth, endOfMonth, TOP_PLAYER_LIMIT);
    }

    public LowestProbabilityWinnerResponse getLowestProbabilityWinner() {
        final LocalDateTime startOfMonth = getStartOfMonth();
        final LocalDateTime endOfMonth = getEndOfMonth();

        return dashboardStatisticsRepository.findLowestProbabilityWinner(startOfMonth, endOfMonth, TOP_PLAYER_LIMIT)
                .orElse(null);
    }

    public List<GamePlayCountResponse> getGamePlayCounts() {
        final LocalDateTime startOfMonth = getStartOfMonth();
        final LocalDateTime endOfMonth = getEndOfMonth();

        return dashboardStatisticsRepository.findGamePlayCountByMonth(startOfMonth, endOfMonth);
    }

    public List<RacingGameTopPlayerResponse> getRacingGameTopPlayers() {
        final LocalDateTime startOfMonth = getStartOfMonth();
        final LocalDateTime endOfMonth = getEndOfMonth();

        return dashboardStatisticsRepository.findRacingGameTopPlayers(startOfMonth, endOfMonth, TOP_PLAYER_LIMIT);
    }

    public List<BlockStackingTopPlayerResponse> getBlockStackingTopPlayers() {
        final LocalDateTime startOfMonth = getStartOfMonth();
        final LocalDateTime endOfMonth = getEndOfMonth();

        return dashboardStatisticsRepository.findBlockStackingTopPlayers(startOfMonth, endOfMonth, TOP_PLAYER_LIMIT);
    }

    private LocalDateTime getStartOfMonth() {
        return LocalDate.now(clock).withDayOfMonth(1).atStartOfDay();
    }

    private LocalDateTime getEndOfMonth() {
        return getStartOfMonth().plusMonths(1).minusNanos(1);
    }
}
