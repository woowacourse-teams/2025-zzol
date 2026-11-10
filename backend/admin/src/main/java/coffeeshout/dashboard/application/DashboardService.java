package coffeeshout.dashboard.application;

import coffeeshout.dashboard.domain.BlindTimerTopPlayerResponse;
import coffeeshout.dashboard.domain.BlockStackingTopPlayerResponse;
import coffeeshout.dashboard.domain.GamePlayCountResponse;
import coffeeshout.dashboard.domain.LowestProbabilityWinnerResponse;
import coffeeshout.dashboard.domain.RacingGameTopPlayerResponse;
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
import java.util.function.Function;
import java.util.function.UnaryOperator;
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
        final LocalDateTime startOfMonth = getStartOfMonth();
        final LocalDateTime endOfMonth = getEndOfMonth();

        return maskUnaudited(
                dashboardStatisticsRepository.findTopWinnersBetween(startOfMonth, endOfMonth, TOP_PLAYER_LIMIT),
                TopWinnerResponse::nickname,
                TopWinnerResponse::mask);
    }

    public LowestProbabilityWinnerResponse getLowestProbabilityWinner() {
        final LocalDateTime startOfMonth = getStartOfMonth();
        final LocalDateTime endOfMonth = getEndOfMonth();

        return dashboardStatisticsRepository
                .findLowestProbabilityWinner(startOfMonth, endOfMonth, TOP_PLAYER_LIMIT)
                .map(winner -> new LowestProbabilityWinnerResponse(
                        winner.probability(),
                        maskUnaudited(
                                winner.players(),
                                LowestProbabilityWinnerResponse.PlayerInfo::nickname,
                                LowestProbabilityWinnerResponse.PlayerInfo::mask)))
                .orElse(LowestProbabilityWinnerResponse.empty());
    }

    public List<GamePlayCountResponse> getGamePlayCounts() {
        final LocalDateTime startOfMonth = getStartOfMonth();
        final LocalDateTime endOfMonth = getEndOfMonth();

        return dashboardStatisticsRepository.findGamePlayCountByMonth(startOfMonth, endOfMonth);
    }

    public List<RacingGameTopPlayerResponse> getRacingGameTopPlayers() {
        final LocalDateTime startOfMonth = getStartOfMonth();
        final LocalDateTime endOfMonth = getEndOfMonth();

        return maskUnaudited(
                dashboardStatisticsRepository.findRacingGameTopPlayers(startOfMonth, endOfMonth, TOP_PLAYER_LIMIT),
                RacingGameTopPlayerResponse::playerName,
                RacingGameTopPlayerResponse::mask);
    }

    public List<BlockStackingTopPlayerResponse> getBlockStackingTopPlayers() {
        final LocalDateTime startOfMonth = getStartOfMonth();
        final LocalDateTime endOfMonth = getEndOfMonth();

        return maskUnaudited(
                dashboardStatisticsRepository.findBlockStackingTopPlayers(startOfMonth, endOfMonth, TOP_PLAYER_LIMIT),
                BlockStackingTopPlayerResponse::playerName,
                BlockStackingTopPlayerResponse::mask);
    }

    public List<WormGameTopPlayerResponse> getWormGameTopPlayers() {
        final LocalDateTime startOfMonth = getStartOfMonth();
        final LocalDateTime endOfMonth = getEndOfMonth();

        return maskUnaudited(
                dashboardStatisticsRepository.findWormGameTopPlayers(startOfMonth, endOfMonth, TOP_PLAYER_LIMIT),
                WormGameTopPlayerResponse::playerName,
                WormGameTopPlayerResponse::mask);
    }

    public List<SpeedTouchTopPlayerResponse> getSpeedTouchTopPlayers() {
        final LocalDateTime startOfMonth = getStartOfMonth();
        final LocalDateTime endOfMonth = getEndOfMonth();

        return maskUnaudited(
                dashboardStatisticsRepository.findSpeedTouchTopPlayers(startOfMonth, endOfMonth, TOP_PLAYER_LIMIT),
                SpeedTouchTopPlayerResponse::playerName,
                SpeedTouchTopPlayerResponse::mask);
    }

    public List<BlindTimerTopPlayerResponse> getBlindTimerTopPlayers() {
        final LocalDateTime startOfMonth = getStartOfMonth();
        final LocalDateTime endOfMonth = getEndOfMonth();

        return maskUnaudited(
                dashboardStatisticsRepository.findBlindTimerTopPlayers(startOfMonth, endOfMonth, TOP_PLAYER_LIMIT),
                BlindTimerTopPlayerResponse::playerName,
                BlindTimerTopPlayerResponse::mask);
    }

    /**
     * 순위와 기록은 바로 보여 주고, 검열을 통과하지 않은 닉네임만 가린다(#1906).
     *
     * <p>랭킹 쿼리에 검열 테이블을 JOIN하지 않는다. 쿼리는 튜닝돼 있고, 검열 상태는 조회된 몇 건의 닉네임만
     * 따로 읽으면 된다.
     */
    private <T> List<T> maskUnaudited(List<T> rows, Function<T, String> nickname, UnaryOperator<T> mask) {
        final Set<String> revealable = nicknameExposureService.findRevealable(
                rows.stream().map(nickname).toList());
        return rows.stream()
                .map(row -> isRevealable(revealable, nickname.apply(row)) ? row : mask.apply(row))
                .toList();
    }

    private boolean isRevealable(Set<String> revealable, String nickname) {
        return nickname != null && revealable.contains(nickname);
    }

    private LocalDateTime getStartOfMonth() {
        return LocalDate.now(clock).withDayOfMonth(1).atStartOfDay();
    }

    private LocalDateTime getEndOfMonth() {
        return getStartOfMonth().plusMonths(1).minusNanos(1);
    }
}
