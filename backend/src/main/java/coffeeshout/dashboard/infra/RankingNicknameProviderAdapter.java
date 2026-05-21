package coffeeshout.dashboard.infra;

import coffeeshout.dashboard.domain.RacingGameTopPlayerResponse;
import coffeeshout.dashboard.domain.TopWinnerResponse;
import coffeeshout.dashboard.domain.repository.DashboardStatisticsRepository;
import coffeeshout.room.application.port.RankingNicknameProvider;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RankingNicknameProviderAdapter implements RankingNicknameProvider {

    private final DashboardStatisticsRepository dashboardRepository;

    @Override
    public Set<String> findRankingNicknamesBetween(LocalDateTime start, LocalDateTime end, int limit) {
        final Set<String> nicknames = new HashSet<>();

        dashboardRepository.findTopWinnersBetween(start, end, limit)
                .stream()
                .map(TopWinnerResponse::nickname)
                .forEach(nicknames::add);

        dashboardRepository.findRacingGameTopPlayers(start, end, limit)
                .stream()
                .map(RacingGameTopPlayerResponse::playerName)
                .forEach(nicknames::add);

        return nicknames;
    }
}
