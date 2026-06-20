package coffeeshout.dashboard.infra.persistence;

import coffeeshout.dashboard.domain.BlindTimerTopPlayerResponse;
import coffeeshout.dashboard.domain.BlockStackingTopPlayerResponse;
import coffeeshout.dashboard.domain.GamePlayCountResponse;
import coffeeshout.dashboard.domain.LowestProbabilityWinnerResponse;
import coffeeshout.dashboard.domain.RacingGameTopPlayerResponse;
import coffeeshout.dashboard.domain.SpeedTouchTopPlayerResponse;
import coffeeshout.dashboard.domain.TopWinnerResponse;
import coffeeshout.dashboard.domain.repository.DashboardStatisticsRepository;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.infra.persistence.QMiniGameEntity;
import coffeeshout.minigame.infra.persistence.QMiniGameResultEntity;
import coffeeshout.room.infra.persistence.QPlayerEntity;
import coffeeshout.room.infra.persistence.QRoomEntity;
import coffeeshout.room.infra.persistence.QRouletteResultEntity;
import coffeeshout.user.infra.persistence.QUserEntity;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class QueryDslDashboardStatisticsRepository implements DashboardStatisticsRepository {

    private static final QRouletteResultEntity ROULETTE_RESULT = QRouletteResultEntity.rouletteResultEntity;
    private static final QPlayerEntity PLAYER = QPlayerEntity.playerEntity;
    private static final QUserEntity USER = QUserEntity.userEntity;
    private static final QMiniGameEntity MINI_GAME = QMiniGameEntity.miniGameEntity;
    private static final QMiniGameResultEntity MINI_GAME_RESULT = QMiniGameResultEntity.miniGameResultEntity;
    private static final QRoomEntity ROOM = QRoomEntity.roomEntity;

    private final JPAQueryFactory queryFactory;

    @Override
    public List<TopWinnerResponse> findTopWinnersBetween(
            LocalDateTime startDate,
            LocalDateTime endDate,
            int limit
    ) {
        return queryFactory
                .select(Projections.constructor(
                        TopWinnerResponse.class,
                        USER.nickname,
                        USER.userCode,
                        ROULETTE_RESULT.count()
                ))
                .from(ROULETTE_RESULT)
                .join(ROULETTE_RESULT.winner, PLAYER)
                .join(USER).on(USER.id.eq(PLAYER.userId))
                .where(ROULETTE_RESULT.createdAt.between(startDate, endDate))
                .groupBy(PLAYER.userId)
                .orderBy(ROULETTE_RESULT.count().desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public Optional<LowestProbabilityWinnerResponse> findLowestProbabilityWinner(
            LocalDateTime startDate,
            LocalDateTime endDate,
            int limit
    ) {
        final Integer minProbability = queryFactory
                .select(ROULETTE_RESULT.winnerProbability.min())
                .from(ROULETTE_RESULT)
                .join(ROULETTE_RESULT.winner, PLAYER)
                .join(USER).on(USER.id.eq(PLAYER.userId))
                .where(ROULETTE_RESULT.createdAt.between(startDate, endDate))
                .fetchOne();

        if (minProbability == null) {
            return Optional.empty();
        }

        final List<Tuple> results = queryFactory
                .select(
                        ROULETTE_RESULT.winnerProbability,
                        USER.nickname,
                        USER.userCode
                )
                .from(ROULETTE_RESULT)
                .join(ROULETTE_RESULT.winner, PLAYER)
                .join(USER).on(USER.id.eq(PLAYER.userId))
                .where(
                        ROULETTE_RESULT.createdAt.between(startDate, endDate),
                        ROULETTE_RESULT.winnerProbability.eq(minProbability)
                )
                .distinct()
                .orderBy(USER.nickname.asc())
                .limit(limit)
                .fetch();

        final List<LowestProbabilityWinnerResponse.PlayerInfo> players = results.stream()
                .map(tuple -> new LowestProbabilityWinnerResponse.PlayerInfo(
                        tuple.get(USER.nickname),
                        tuple.get(USER.userCode)
                ))
                .toList();

        return Optional.of(LowestProbabilityWinnerResponse.of(minProbability, players));
    }

    @Override
    public List<GamePlayCountResponse> findGamePlayCountByMonth(LocalDateTime startDate, LocalDateTime endDate) {
        return queryFactory
                .select(Projections.constructor(
                        GamePlayCountResponse.class,
                        MINI_GAME.miniGameType,
                        MINI_GAME.count()
                ))
                .from(MINI_GAME)
                .join(MINI_GAME.roomSession, ROOM)
                .where(ROOM.createdAt.between(startDate, endDate))
                .groupBy(MINI_GAME.miniGameType)
                .orderBy(MINI_GAME.count().desc())
                .fetch();
    }

    @Override
    public List<RacingGameTopPlayerResponse> findRacingGameTopPlayers(
            LocalDateTime startDate,
            LocalDateTime endDate,
            int limit
    ) {
        return findTopPlayersByMinScore(
                MiniGameType.RACING_GAME,
                startDate,
                endDate,
                limit,
                null,
                RacingGameTopPlayerResponse::new
        );
    }

    @Override
    public List<SpeedTouchTopPlayerResponse> findSpeedTouchTopPlayers(
            LocalDateTime startDate,
            LocalDateTime endDate,
            int limit
    ) {
        // 완주 점수는 완주 시간(ms). DNF는 SpeedTouchScore에서 DNF_BASE(1_000_000_000) - progress로
        // 10^9 부근 값이므로, 이 경계 미만(실제 완주 기록)만 집계해 미완주 기록을 TOP에서 제외한다.
        final long finishScoreCeiling = 1_000_000L;

        return findTopPlayersByMinScore(
                MiniGameType.SPEED_TOUCH,
                startDate,
                endDate,
                limit,
                MINI_GAME_RESULT.score.lt(finishScoreCeiling),
                SpeedTouchTopPlayerResponse::new
        );
    }

    @Override
    public List<BlindTimerTopPlayerResponse> findBlindTimerTopPlayers(
            LocalDateTime startDate,
            LocalDateTime endDate,
            int limit
    ) {
        // 점수는 목표 시간과의 오차(ms, 작을수록 가까움). 타임아웃은 BlindTimerScore에서 TIMEOUT_PENALTY(999_999_999)로
        // 항상 꼴등 그룹이므로, 이 경계 미만(정상 STOP, 최대 오차 약 20초)만 집계해 타임아웃 기록을 TOP에서 제외한다.
        final long normalStopScoreCeiling = 1_000_000L;

        return findTopPlayersByMinScore(
                MiniGameType.BLIND_TIMER,
                startDate,
                endDate,
                limit,
                MINI_GAME_RESULT.score.lt(normalStopScoreCeiling),
                BlindTimerTopPlayerResponse::new
        );
    }

    @Override
    public List<BlockStackingTopPlayerResponse> findBlockStackingTopPlayers(
            LocalDateTime startDate,
            LocalDateTime endDate,
            int limit
    ) {
        return queryFactory
                .select(Projections.constructor(
                        BlockStackingTopPlayerResponse.class,
                        PLAYER.playerName,
                        MINI_GAME_RESULT.score.max()
                ))
                .from(MINI_GAME_RESULT)
                .join(MINI_GAME_RESULT.player, PLAYER)
                .where(
                        MINI_GAME_RESULT.miniGameType.eq(MiniGameType.BLOCK_STACKING),
                        MINI_GAME_RESULT.createdAt.between(startDate, endDate)
                )
                .groupBy(PLAYER.playerName)
                .orderBy(MINI_GAME_RESULT.score.max().desc())
                .limit(limit)
                .fetch();
    }

    /**
     * 최단/최소 점수가 우수한 미니게임의 TOP 플레이어를 조회한다(RacingGame·SpeedTouch·BlindTimer 공통).
     * player_id로 집계 후 별도 쿼리로 이름을 매핑하는 2-스텝 방식으로, 집계 단계의 JOIN을 피한다.
     *
     * @param scoreFilter 점수 경계 등 추가 필터(없으면 null — QueryDSL이 null 조건을 무시한다)
     * @param mapper      (playerName, bestScore) → 응답 DTO 생성자
     */
    private <T> List<T> findTopPlayersByMinScore(
            MiniGameType miniGameType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            int limit,
            BooleanExpression scoreFilter,
            BiFunction<String, Long, T> mapper
    ) {
        final List<Tuple> aggregations = queryFactory
                .select(
                        MINI_GAME_RESULT.player.id,
                        MINI_GAME_RESULT.score.min()
                )
                .from(MINI_GAME_RESULT)
                .where(
                        MINI_GAME_RESULT.miniGameType.eq(miniGameType),
                        MINI_GAME_RESULT.createdAt.between(startDate, endDate),
                        scoreFilter
                )
                .groupBy(MINI_GAME_RESULT.player.id)
                .orderBy(MINI_GAME_RESULT.score.min().asc(), MINI_GAME_RESULT.player.id.asc())
                .limit(limit)
                .fetch();

        if (aggregations.isEmpty()) {
            return List.of();
        }

        final List<Long> playerIds = aggregations.stream()
                .map(tuple -> tuple.get(MINI_GAME_RESULT.player.id))
                .toList();

        final Map<Long, String> playerNameMap = findPlayerNames(playerIds);

        return aggregations.stream()
                .map(tuple -> mapper.apply(
                        playerNameMap.get(tuple.get(MINI_GAME_RESULT.player.id)),
                        tuple.get(MINI_GAME_RESULT.score.min())
                ))
                .toList();
    }

    private Map<Long, String> findPlayerNames(List<Long> playerIds) {
        return queryFactory
                .select(PLAYER.id, PLAYER.playerName)
                .from(PLAYER)
                .where(PLAYER.id.in(playerIds))
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(PLAYER.id),
                        tuple -> tuple.get(PLAYER.playerName)
                ));
    }
}
