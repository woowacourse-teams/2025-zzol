package coffeeshout.dashboard.infra;

import coffeeshout.dashboard.domain.RacingGameTopPlayerResponse;
import coffeeshout.dashboard.domain.TopWinnerResponse;
import coffeeshout.dashboard.domain.repository.DashboardStatisticsRepository;
import coffeeshout.room.domain.player.RankedNicknameReader;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DashboardRankedNicknameReader implements RankedNicknameReader {

    private final DashboardStatisticsRepository dashboardRepository;

    @Override
    public Set<String> findRankedNicknames(LocalDateTime start, LocalDateTime end, int limit) {
        return Stream.concat(
                dashboardRepository.findTopWinnersBetween(start, end, limit)
                        .stream().map(TopWinnerResponse::nickname),
                dashboardRepository.findRacingGameTopPlayers(start, end, limit)
                        .stream().map(RacingGameTopPlayerResponse::playerName)
        ).collect(Collectors.toUnmodifiableSet());
    }
}
