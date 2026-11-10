package coffeeshout.dashboard.application;

import coffeeshout.dashboard.domain.BlindTimerTopPlayerResponse;
import coffeeshout.dashboard.domain.BlockStackingTopPlayerResponse;
import coffeeshout.dashboard.domain.GamePlayCountResponse;
import coffeeshout.dashboard.domain.LowestProbabilityWinnerResponse;
import coffeeshout.dashboard.domain.MemberScore;
import coffeeshout.dashboard.domain.PlayerScore;
import coffeeshout.dashboard.domain.RacingGameTopPlayerResponse;
import coffeeshout.dashboard.domain.RankedMember;
import coffeeshout.dashboard.domain.RankedPlayerName;
import coffeeshout.dashboard.domain.SpeedTouchTopPlayerResponse;
import coffeeshout.dashboard.domain.TopWinnerResponse;
import coffeeshout.dashboard.domain.WormGameTopPlayerResponse;
import coffeeshout.dashboard.domain.repository.DashboardStatisticsRepository;
import coffeeshout.profanity.application.NicknameExposureService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DashboardService {

    private static final int TOP_PLAYER_LIMIT = 5;

    private final DashboardStatisticsRepository dashboardStatisticsRepository;
    private final NicknameExposureService nicknameExposureService;
    private final Clock clock;

    public List<TopWinnerResponse> getTop5Winners() {
        return rankMembers(
                dashboardStatisticsRepository.findTopWinnersBetween(
                        getStartOfMonth(), getEndOfMonth(), TOP_PLAYER_LIMIT),
                TopWinnerResponse::new);
    }

    public LowestProbabilityWinnerResponse getLowestProbabilityWinner() {
        final List<MemberScore> winners = dashboardStatisticsRepository.findLowestProbabilityWinners(
                getStartOfMonth(), getEndOfMonth(), TOP_PLAYER_LIMIT);
        if (winners.isEmpty()) {
            return LowestProbabilityWinnerResponse.empty();
        }
        return LowestProbabilityWinnerResponse.of(
                winners.getFirst().score(), rankMembers(winners, (member, probability) -> member));
    }

    public List<GamePlayCountResponse> getGamePlayCounts() {
        return dashboardStatisticsRepository.findGamePlayCountByMonth(getStartOfMonth(), getEndOfMonth());
    }

    public List<RacingGameTopPlayerResponse> getRacingGameTopPlayers() {
        return rankPlayers(
                dashboardStatisticsRepository.findRacingGameTopPlayers(
                        getStartOfMonth(), getEndOfMonth(), TOP_PLAYER_LIMIT),
                RacingGameTopPlayerResponse::new);
    }

    public List<BlockStackingTopPlayerResponse> getBlockStackingTopPlayers() {
        return rankPlayers(
                dashboardStatisticsRepository.findBlockStackingTopPlayers(
                        getStartOfMonth(), getEndOfMonth(), TOP_PLAYER_LIMIT),
                BlockStackingTopPlayerResponse::new);
    }

    public List<WormGameTopPlayerResponse> getWormGameTopPlayers() {
        return rankPlayers(
                dashboardStatisticsRepository.findWormGameTopPlayers(
                        getStartOfMonth(), getEndOfMonth(), TOP_PLAYER_LIMIT),
                WormGameTopPlayerResponse::new);
    }

    public List<SpeedTouchTopPlayerResponse> getSpeedTouchTopPlayers() {
        return rankPlayers(
                dashboardStatisticsRepository.findSpeedTouchTopPlayers(
                        getStartOfMonth(), getEndOfMonth(), TOP_PLAYER_LIMIT),
                SpeedTouchTopPlayerResponse::new);
    }

    public List<BlindTimerTopPlayerResponse> getBlindTimerTopPlayers() {
        return rankPlayers(
                dashboardStatisticsRepository.findBlindTimerTopPlayers(
                        getStartOfMonth(), getEndOfMonth(), TOP_PLAYER_LIMIT),
                BlindTimerTopPlayerResponse::new);
    }

    /**
     * 순위와 기록은 바로 보여 주고, 검열을 통과하지 않은 닉네임만 가린다(#1906).
     *
     * <p>랭킹 쿼리에 검열 테이블을 JOIN하지 않는다. 쿼리는 튜닝돼 있고, 검열 상태는 조회된 몇 건의 닉네임만
     * 따로 읽으면 된다. 응답은 원문 대신 {@link RankedPlayerName}·{@link RankedMember}를 받으므로 가림을 건너뛸 수 없다.
     */
    private <T> List<T> rankPlayers(List<PlayerScore> rows, BiFunction<RankedPlayerName, Long, T> toResponse) {
        final Set<String> revealable = nicknameExposureService.findRevealable(
                rows.stream().map(PlayerScore::playerName).toList());
        return rows.stream()
                .map(row -> toResponse.apply(RankedPlayerName.of(row.playerName(), revealable), row.score()))
                .toList();
    }

    private <T> List<T> rankMembers(List<MemberScore> rows, BiFunction<RankedMember, Long, T> toResponse) {
        final Set<String> revealable = nicknameExposureService.findRevealable(
                rows.stream().map(MemberScore::nickname).toList());
        return rows.stream()
                .map(row -> toResponse.apply(RankedMember.of(row.nickname(), row.userCode(), revealable), row.score()))
                .toList();
    }

    private LocalDateTime getStartOfMonth() {
        return LocalDate.now(clock).withDayOfMonth(1).atStartOfDay();
    }

    private LocalDateTime getEndOfMonth() {
        return getStartOfMonth().plusMonths(1).minusNanos(1);
    }
}
