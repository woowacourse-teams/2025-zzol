package coffeeshout.dashboard.application;

import coffeeshout.dashboard.domain.BlindTimerTopPlayerResponse;
import coffeeshout.dashboard.domain.BlockStackingTopPlayerResponse;
import coffeeshout.dashboard.domain.GamePlayCountResponse;
import coffeeshout.dashboard.domain.LowestProbabilityWinnerResponse;
import coffeeshout.dashboard.domain.MaskedNickname;
import coffeeshout.dashboard.domain.PlayerScore;
import coffeeshout.dashboard.domain.RacingGameTopPlayerResponse;
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
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DashboardService {

    private static final int TOP_PLAYER_LIMIT = 5;
    // ponytail: 상위 10명 중 판정 전 회원이 6명 이상이면 5명을 못 채운다. 모자라면 후보 수를 늘린다.
    private static final int TOP_WINNER_CANDIDATE_LIMIT = TOP_PLAYER_LIMIT * 2;

    private final DashboardStatisticsRepository dashboardStatisticsRepository;
    private final NicknameExposureService nicknameExposureService;
    private final Clock clock;

    /**
     * 당첨자 슬라이드는 판정 전 회원을 가리지 않고 뺀다(#1906). 사람을 보여 주는 하이라이트라 문구가 끼면 어색하고,
     * 회원 닉네임은 가입·변경 때 검열 큐에 들어가 판정 전인 경우가 드물다. 빠진 자리는 다음 순위로 채운다.
     */
    public List<TopWinnerResponse> getTop5Winners() {
        final List<TopWinnerResponse> candidates = dashboardStatisticsRepository.findTopWinnersBetween(
                getStartOfMonth(), getEndOfMonth(), TOP_WINNER_CANDIDATE_LIMIT);
        return onlyRevealable(candidates, TopWinnerResponse::nickname).stream()
                .limit(TOP_PLAYER_LIMIT)
                .toList();
    }

    /** 공동 당첨자 중 판정 전 회원은 뺀다. 모두 빠지면 다음 확률을 최저라고 보여 주지 않고 빈 상태로 둔다(#1906). */
    public LowestProbabilityWinnerResponse getLowestProbabilityWinner() {
        return dashboardStatisticsRepository
                .findLowestProbabilityWinner(getStartOfMonth(), getEndOfMonth(), TOP_PLAYER_LIMIT)
                .map(winner -> new LowestProbabilityWinnerResponse(
                        winner.probability(),
                        onlyRevealable(winner.players(), LowestProbabilityWinnerResponse.PlayerInfo::nickname)))
                .filter(winner -> !winner.players().isEmpty())
                .orElse(LowestProbabilityWinnerResponse.empty());
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
     * 따로 읽으면 된다. 응답은 원문 대신 {@link RankedPlayerName}을 받으므로 가림을 건너뛸 수 없다.
     */
    private <T> List<T> rankPlayers(List<PlayerScore> rows, BiFunction<RankedPlayerName, Long, T> toResponse) {
        final Set<String> revealable = nicknameExposureService.findRevealable(
                rows.stream().map(PlayerScore::playerName).toList());
        return rows.stream()
                .map(row -> toResponse.apply(RankedPlayerName.of(row.playerName(), revealable), row.score()))
                .toList();
    }

    private <T> List<T> onlyRevealable(List<T> rows, Function<T, String> nickname) {
        final Set<String> revealable = nicknameExposureService.findRevealable(
                rows.stream().map(nickname).toList());
        return rows.stream()
                .filter(row -> MaskedNickname.isRevealable(nickname.apply(row), revealable))
                .toList();
    }

    private LocalDateTime getStartOfMonth() {
        return LocalDate.now(clock).withDayOfMonth(1).atStartOfDay();
    }

    private LocalDateTime getEndOfMonth() {
        return getStartOfMonth().plusMonths(1).minusNanos(1);
    }
}
