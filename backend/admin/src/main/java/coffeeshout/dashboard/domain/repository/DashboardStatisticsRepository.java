package coffeeshout.dashboard.domain.repository;

import coffeeshout.dashboard.domain.GamePlayCountResponse;
import coffeeshout.dashboard.domain.LowestProbabilityWinnerResponse;
import coffeeshout.dashboard.domain.PlayerScore;
import coffeeshout.dashboard.domain.TopWinnerResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** 미니게임 랭킹은 원본 행으로 돌려준다. 검열 전 이름을 가리는 일은 응답을 조립하는 서비스가 맡는다(#1906). */
public interface DashboardStatisticsRepository {

    List<TopWinnerResponse> findTopWinnersBetween(LocalDateTime startDate, LocalDateTime endDate, int limit);

    Optional<LowestProbabilityWinnerResponse> findLowestProbabilityWinner(
            LocalDateTime startDate, LocalDateTime endDate, int limit);

    List<GamePlayCountResponse> findGamePlayCountByMonth(LocalDateTime startDate, LocalDateTime endDate);

    List<PlayerScore> findRacingGameTopPlayers(LocalDateTime startDate, LocalDateTime endDate, int limit);

    List<PlayerScore> findBlockStackingTopPlayers(LocalDateTime startDate, LocalDateTime endDate, int limit);

    List<PlayerScore> findWormGameTopPlayers(LocalDateTime startDate, LocalDateTime endDate, int limit);

    List<PlayerScore> findSpeedTouchTopPlayers(LocalDateTime startDate, LocalDateTime endDate, int limit);

    List<PlayerScore> findBlindTimerTopPlayers(LocalDateTime startDate, LocalDateTime endDate, int limit);
}
