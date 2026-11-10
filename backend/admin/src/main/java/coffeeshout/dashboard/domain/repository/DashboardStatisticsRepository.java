package coffeeshout.dashboard.domain.repository;

import coffeeshout.dashboard.domain.GamePlayCountResponse;
import coffeeshout.dashboard.domain.MemberScore;
import coffeeshout.dashboard.domain.PlayerScore;
import java.time.LocalDateTime;
import java.util.List;

/** 랭킹은 원본 행으로 돌려준다. 검열 전 닉네임을 가리는 일은 응답을 조립하는 서비스가 맡는다(#1906). */
public interface DashboardStatisticsRepository {

    /** 점수는 당첨 횟수다. */
    List<MemberScore> findTopWinnersBetween(LocalDateTime startDate, LocalDateTime endDate, int limit);

    /** 가장 낮은 확률로 당첨된 회원들이다. 점수는 그 확률(DB 단위)이고 모든 행이 같다. 없으면 빈 목록이다. */
    List<MemberScore> findLowestProbabilityWinners(LocalDateTime startDate, LocalDateTime endDate, int limit);

    List<GamePlayCountResponse> findGamePlayCountByMonth(LocalDateTime startDate, LocalDateTime endDate);

    List<PlayerScore> findRacingGameTopPlayers(LocalDateTime startDate, LocalDateTime endDate, int limit);

    List<PlayerScore> findBlockStackingTopPlayers(LocalDateTime startDate, LocalDateTime endDate, int limit);

    List<PlayerScore> findWormGameTopPlayers(LocalDateTime startDate, LocalDateTime endDate, int limit);

    List<PlayerScore> findSpeedTouchTopPlayers(LocalDateTime startDate, LocalDateTime endDate, int limit);

    List<PlayerScore> findBlindTimerTopPlayers(LocalDateTime startDate, LocalDateTime endDate, int limit);
}
