package coffeeshout.dashboard.infra.persistence;

import coffeeshout.dashboard.domain.GamePlayCountResponse;
import coffeeshout.dashboard.domain.LowestProbabilityWinnerResponse;
import coffeeshout.dashboard.domain.TopWinnerResponse;
import coffeeshout.dashboard.domain.repository.DashboardStatisticsRepository;
import coffeeshout.minigame.infra.persistence.QMiniGameEntity;
import coffeeshout.room.infra.persistence.QPlayerEntity;
import coffeeshout.room.infra.persistence.QRoomEntity;
import coffeeshout.room.infra.persistence.QRouletteResultEntity;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class QueryDslDashboardStatisticsRepository implements DashboardStatisticsRepository {

    private static final QRouletteResultEntity ROULETTE_RESULT = QRouletteResultEntity.rouletteResultEntity;
    private static final QPlayerEntity PLAYER = QPlayerEntity.playerEntity;
    private static final QMiniGameEntity MINI_GAME = QMiniGameEntity.miniGameEntity;
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
                        PLAYER.playerName,
                        ROULETTE_RESULT.count()
                ))
                .from(ROULETTE_RESULT)
                .join(ROULETTE_RESULT.winner, PLAYER)
                .where(ROULETTE_RESULT.createdAt.between(startDate, endDate))
                .groupBy(PLAYER.playerName)
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
        // 서브쿼리로 최소 확률 찾기
        final QRouletteResultEntity subRouletteResult = new QRouletteResultEntity("subRouletteResult");

        final List<com.querydsl.core.Tuple> results = queryFactory
                .select(
                        ROULETTE_RESULT.winnerProbability,
                        PLAYER.playerName
                )
                .from(ROULETTE_RESULT)
                .join(ROULETTE_RESULT.winner, PLAYER)
                .where(
                        ROULETTE_RESULT.createdAt.between(startDate, endDate),
                        ROULETTE_RESULT.winnerProbability.eq(
                                queryFactory
                                        .select(subRouletteResult.winnerProbability.min())
                                        .from(subRouletteResult)
                                        .where(subRouletteResult.createdAt.between(startDate, endDate))
                        )
                )
                .distinct()
                .orderBy(PLAYER.playerName.asc())
                .limit(limit)
                .fetch();

        if (results.isEmpty()) {
            return Optional.empty();
        }

        final Integer minProbability = results.getFirst().get(ROULETTE_RESULT.winnerProbability);
        final List<String> nicknames = results.stream()
                .map(tuple -> tuple.get(PLAYER.playerName))
                .toList();

        return Optional.of(LowestProbabilityWinnerResponse.of(minProbability, nicknames));
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
}
