package coffeeshout.dashboard.infra;

import coffeeshout.dashboard.domain.RacingGameTopPlayerResponse;
import coffeeshout.dashboard.domain.TopWinnerResponse;
import coffeeshout.dashboard.domain.repository.DashboardStatisticsRepository;
import coffeeshout.room.domain.player.RankedNicknameReader;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DashboardRankedPlayerNicknameAdapter implements RankedNicknameReader {

    private final DashboardStatisticsRepository dashboardRepository;

    @Override
    public Set<String> findRankedNicknames(LocalDateTime start, LocalDateTime end, int limit) {
        final Set<String> nicknames = new HashSet<>();
        dashboardRepository.findTopWinnersBetween(start, end, limit)
                .stream().map(TopWinnerResponse::nickname).forEach(nicknames::add);
        dashboardRepository.findRacingGameTopPlayers(start, end, limit)
                .stream().map(RacingGameTopPlayerResponse::playerName).forEach(nicknames::add);
        return nicknames;
    }
}
