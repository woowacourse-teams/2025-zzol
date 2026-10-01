package coffeeshout.admin.room.infra.persistence;

import coffeeshout.admin.room.domain.RoomLookupRepository;
import coffeeshout.admin.room.domain.RoomMiniGameResult;
import coffeeshout.admin.room.domain.RoomPlayer;
import coffeeshout.admin.room.domain.RoomRouletteResult;
import coffeeshout.admin.room.domain.RoomSnapshot;
import coffeeshout.admin.room.domain.RoomSummary;
import coffeeshout.minigame.infra.persistence.QMiniGameResultEntity;
import coffeeshout.room.infra.persistence.QPlayerEntity;
import coffeeshout.room.infra.persistence.QRoomEntity;
import coffeeshout.room.infra.persistence.QRouletteResultEntity;
import coffeeshout.user.infra.persistence.QUserEntity;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class QueryDslRoomLookupRepository implements RoomLookupRepository {

    private static final QRoomEntity ROOM = QRoomEntity.roomEntity;
    private static final QPlayerEntity PLAYER = QPlayerEntity.playerEntity;
    private static final QUserEntity USER = QUserEntity.userEntity;
    private static final QRouletteResultEntity ROULETTE = QRouletteResultEntity.rouletteResultEntity;
    private static final QMiniGameResultEntity MINI_GAME_RESULT = QMiniGameResultEntity.miniGameResultEntity;

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<RoomSummary> search(String joinCode, Pageable pageable) {
        final BooleanExpression condition = joinCodeEq(joinCode);

        final List<RoomSummary> content = queryFactory
                .select(Projections.constructor(
                        RoomSummary.class,
                        ROOM.id,
                        ROOM.joinCode,
                        ROOM.roomStatus,
                        ROOM.createdAt,
                        ROOM.finishedAt,
                        // 방마다 참여자 수를 서브쿼리로 센다. 목록에서 "2명짜리 방"을 바로 걸러내려면
                        // 이 값이 필요하고, 화면이 방마다 다시 물어보게 하면 N+1 이 된다.
                        JPAExpressions.select(PLAYER.count()).from(PLAYER).where(PLAYER.roomSession.id.eq(ROOM.id))))
                .from(ROOM)
                .where(condition)
                .orderBy(ROOM.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        final Long total =
                queryFactory.select(ROOM.count()).from(ROOM).where(condition).fetchOne();
        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    @Override
    public Optional<RoomSummary> findSummaryById(Long roomId) {
        return Optional.ofNullable(queryFactory
                .select(Projections.constructor(
                        RoomSummary.class,
                        ROOM.id,
                        ROOM.joinCode,
                        ROOM.roomStatus,
                        ROOM.createdAt,
                        ROOM.finishedAt,
                        JPAExpressions.select(PLAYER.count()).from(PLAYER).where(PLAYER.roomSession.id.eq(ROOM.id))))
                .from(ROOM)
                .where(ROOM.id.eq(roomId))
                .fetchOne());
    }

    @Override
    public List<RoomPlayer> findPlayers(Long roomId) {
        // 게스트는 user_id 가 없으므로 leftJoin 이다. innerJoin 이면 게스트가 통째로 사라져
        // "참여자 4명인데 3명만 보인다"가 된다.
        return queryFactory
                .select(Projections.constructor(
                        RoomPlayer.class,
                        PLAYER.id,
                        PLAYER.playerName,
                        PLAYER.playerType,
                        PLAYER.userId,
                        USER.nickname,
                        USER.userCode,
                        PLAYER.createdAt))
                .from(PLAYER)
                .leftJoin(USER)
                .on(USER.id.eq(PLAYER.userId))
                .where(PLAYER.roomSession.id.eq(roomId))
                .orderBy(PLAYER.createdAt.asc())
                .fetch();
    }

    @Override
    public List<RoomMiniGameResult> findMiniGameResults(Long roomId) {
        return queryFactory
                .select(Projections.constructor(
                        RoomMiniGameResult.class,
                        MINI_GAME_RESULT.miniGameType,
                        MINI_GAME_RESULT.playerId,
                        PLAYER.playerName,
                        MINI_GAME_RESULT.rank,
                        MINI_GAME_RESULT.score,
                        MINI_GAME_RESULT.createdAt))
                .from(MINI_GAME_RESULT)
                .join(PLAYER)
                .on(PLAYER.id.eq(MINI_GAME_RESULT.playerId))
                .where(PLAYER.roomSession.id.eq(roomId))
                .orderBy(MINI_GAME_RESULT.createdAt.asc(), MINI_GAME_RESULT.rank.asc())
                .fetch();
    }

    @Override
    public Optional<RoomRouletteResult> findRouletteResult(Long roomId) {
        return Optional.ofNullable(queryFactory
                .select(Projections.constructor(
                        RoomRouletteResult.class,
                        ROULETTE.winner.id,
                        PLAYER.playerName,
                        ROULETTE.winnerProbability,
                        ROULETTE.createdAt))
                .from(ROULETTE)
                .join(ROULETTE.winner, PLAYER)
                .where(ROULETTE.roomSession.id.eq(roomId))
                .fetchFirst());
    }

    /**
     * 인원수를 서브쿼리로 붙인다. player 를 조인하면 참여자 수만큼 방이 불어나 방 수 자체가
     * 틀어진다. 여기서 세는 것은 방이지 참여자가 아니다.
     */
    @Override
    public List<RoomSnapshot> findSnapshots(LocalDateTime from, LocalDateTime to) {
        return queryFactory
                .select(Projections.constructor(
                        RoomSnapshot.class,
                        ROOM.roomStatus,
                        ROOM.createdAt,
                        ROOM.finishedAt,
                        JPAExpressions.select(PLAYER.count()).from(PLAYER).where(PLAYER.roomSession.id.eq(ROOM.id))))
                .from(ROOM)
                .where(ROOM.createdAt.goe(from), ROOM.createdAt.lt(to))
                .fetch();
    }

    private static BooleanExpression joinCodeEq(String joinCode) {
        // null 을 돌려주면 QueryDSL 이 그 조건을 통째로 무시한다. 비어 있으면 전체 조회다.
        return (joinCode == null || joinCode.isBlank())
                ? null
                : ROOM.joinCode.eq(joinCode.trim().toUpperCase());
    }
}
