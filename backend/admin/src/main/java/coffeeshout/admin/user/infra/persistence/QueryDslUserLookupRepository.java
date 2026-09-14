package coffeeshout.admin.user.infra.persistence;

import coffeeshout.admin.user.domain.ProviderCount;
import coffeeshout.admin.user.domain.UserActivity;
import coffeeshout.admin.user.domain.UserListRow;
import coffeeshout.admin.user.domain.UserLookupRepository;
import coffeeshout.admin.user.domain.UserPlayAggregate;
import coffeeshout.admin.user.domain.UserSummary;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.infra.persistence.QMiniGameResultEntity;
import coffeeshout.room.infra.persistence.QPlayerEntity;
import coffeeshout.room.infra.persistence.QRouletteResultEntity;
import coffeeshout.user.infra.persistence.QOAuthAccountEntity;
import coffeeshout.user.infra.persistence.QUserEntity;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class QueryDslUserLookupRepository implements UserLookupRepository {

    private static final QUserEntity USER = QUserEntity.userEntity;
    private static final QPlayerEntity PLAYER = QPlayerEntity.playerEntity;
    private static final QRouletteResultEntity ROULETTE = QRouletteResultEntity.rouletteResultEntity;
    private static final QOAuthAccountEntity OAUTH = QOAuthAccountEntity.oAuthAccountEntity;
    private static final QMiniGameResultEntity RESULT = QMiniGameResultEntity.miniGameResultEntity;

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<UserListRow> search(String keyword, Pageable pageable) {
        // 탈퇴 필터가 없다. @SQLRestriction 이 이미 탈퇴 회원을 걸러내 필터를 둬도 의미가 없다.
        final BooleanExpression condition = keywordMatches(keyword);

        final List<UserListRow> content = queryFactory
                .select(listRowProjection())
                .from(USER)
                .where(condition)
                .orderBy(USER.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        final Map<Long, String> topGames =
                findTopGames(content.stream().map(UserListRow::id).toList());
        final List<UserListRow> merged = content.stream()
                .map(row -> new UserListRow(
                        row.id(),
                        row.userCode(),
                        row.nickname(),
                        row.createdAt(),
                        row.playCount(),
                        topGames.get(row.id()),
                        row.lastPlayedAt()))
                .toList();

        final Long total =
                queryFactory.select(USER.count()).from(USER).where(condition).fetchOne();
        return new PageImpl<>(merged, pageable, total == null ? 0 : total);
    }

    /**
     * 목록 한 줄. 플레이 수와 마지막 참여는 상관 서브쿼리로 붙인다.
     *
     * <p>조인으로 한 번에 세면 한 사람이 여러 판을 한 만큼 행이 불어나 가입일까지 중복되고,
     * 그걸 다시 group by 로 접어야 한다. 스무 줄짜리 목록에서는 서브쿼리 쪽이 읽기도 쉽고
     * 결과도 정확하다.
     *
     * <p>가장 많이 한 게임은 여기서 뽑지 않는다. 줄마다 "group by 해서 1등"을 구하는
     * 서브쿼리는 QueryDSL 로 표현하기도 어렵고 스무 번 돌면 비싸다. 페이지의 유저 id 를
     * 모아 <b>한 번에</b> 집계해 자바에서 붙인다.
     */
    private ConstructorExpression<UserListRow> listRowProjection() {
        return Projections.constructor(
                UserListRow.class,
                USER.id,
                USER.userCode,
                USER.nickname,
                USER.createdAt,
                ExpressionUtils.as(
                        JPAExpressions.select(RESULT.count())
                                .from(RESULT)
                                .where(RESULT.playerId.in(JPAExpressions.select(PLAYER.id)
                                        .from(PLAYER)
                                        .where(PLAYER.userId.eq(USER.id)))),
                        "playCount"),
                Expressions.nullExpression(String.class),
                ExpressionUtils.as(
                        JPAExpressions.select(PLAYER.createdAt.max())
                                .from(PLAYER)
                                .where(PLAYER.userId.eq(USER.id)),
                        "lastPlayedAt"));
    }

    /**
     * 페이지에 실린 사람들의 "가장 많이 한 게임"을 한 번에 구한다.
     *
     * <p>(유저, 게임)별 판 수를 통째로 받아 자바에서 1등을 고른다. 같은 수면 먼저 온 것을
     * 둔다 - 무엇을 고르든 "가장 많이"라는 말은 같고, 순서를 정해 두지 않으면 새로고침마다
     * 값이 바뀌어 화면을 못 믿게 된다.
     */
    private Map<Long, String> findTopGames(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }

        final List<Tuple> rows = queryFactory
                .select(PLAYER.userId, RESULT.miniGameType, RESULT.count())
                .from(RESULT)
                .join(PLAYER)
                .on(PLAYER.id.eq(RESULT.playerId))
                .where(PLAYER.userId.in(userIds))
                .groupBy(PLAYER.userId, RESULT.miniGameType)
                .orderBy(RESULT.count().desc())
                .fetch();

        final Map<Long, String> topByUser = new HashMap<>();
        for (Tuple row : rows) {
            final Long userId = row.get(PLAYER.userId);
            if (userId != null && !topByUser.containsKey(userId)) {
                final MiniGameType type = row.get(RESULT.miniGameType);
                topByUser.put(userId, type == null ? null : type.name());
            }
        }
        return topByUser;
    }

    @Override
    public Optional<UserSummary> findById(Long userId) {
        return Optional.ofNullable(queryFactory
                .select(summaryProjection())
                .from(USER)
                .where(USER.id.eq(userId))
                .fetchOne());
    }

    @Override
    public UserActivity findActivity(Long userId) {
        final Long roomCount = queryFactory
                .select(PLAYER.roomSession.id.countDistinct())
                .from(PLAYER)
                .where(PLAYER.userId.eq(userId))
                .fetchOne();

        final Long winCount = queryFactory
                .select(ROULETTE.count())
                .from(ROULETTE)
                .join(ROULETTE.winner, PLAYER)
                .where(PLAYER.userId.eq(userId))
                .fetchOne();

        return new UserActivity(nullToZero(roomCount), nullToZero(winCount));
    }

    @Override
    public List<String> findProviders(Long userId) {
        return queryFactory
                .select(OAUTH.provider)
                .from(OAUTH)
                .where(OAUTH.user.id.eq(userId))
                .orderBy(OAUTH.linkedAt.asc())
                .fetch();
    }

    @Override
    public long countUsers() {
        final Long total = queryFactory.select(USER.count()).from(USER).fetchOne();
        return nullToZero(total);
    }

    /**
     * 제공자별 연결 수.
     *
     * <p>{@code oauth_account} 만 세지 않고 {@code user} 를 조인한다. 조인하지 않으면
     * {@code UserEntity} 의 {@code @SQLRestriction} 이 걸리지 않아 <b>탈퇴 회원의 연결까지
     * 세어진다</b>. 같은 화면의 회원 수는 탈퇴를 빼고 세므로 두 숫자가 어긋난다.
     */
    @Override
    public List<ProviderCount> countByProvider() {
        return queryFactory
                .select(Projections.constructor(ProviderCount.class, OAUTH.provider, OAUTH.count()))
                .from(OAUTH)
                .join(OAUTH.user, USER)
                .groupBy(OAUTH.provider)
                .orderBy(OAUTH.count().desc())
                .fetch();
    }

    @Override
    public List<Instant> findSignupTimes(Instant from, Instant to) {
        return queryFactory
                .select(USER.createdAt)
                .from(USER)
                .where(USER.createdAt.goe(from), USER.createdAt.lt(to))
                .fetch();
    }

    /**
     * 플레이 수와 마지막 참여를 사람마다 한 줄로 모은다.
     *
     * <p>한 쿼리로 뽑는다. 한때 둘로 나눠 놓고 "조인하면 판 수만큼 player 행이 불어나
     * 참여한 방이 여러 번 세진다"고 적어 두었는데, <b>여기서 세는 것이 방이 아니라서</b>
     * 그 걱정이 성립하지 않았다. 판 수는 {@code mini_game_result} 행을 세는 것이라 불어난
     * 쪽이 곧 세려던 쪽이고, 마지막 참여는 최댓값이라 같은 값이 여러 번 나와도 결과가
     * 바뀌지 않는다. 방 수를 함께 세려 했다면 그때는 정말 나눠야 한다.
     *
     * <p><b>{@code USER} 를 조인하는 것이 핵심이다.</b> {@code player.user_id} 만 보고 세면
     * 탈퇴 회원이 그대로 섞인다. {@code UserEntity} 의 {@code @SQLRestriction} 은 그 엔티티가
     * 쿼리에 등장할 때 붙는 것이라, id 만 들고 다니면 걸리지 않는다. 같은 화면의 회원 수는
     * 탈퇴를 빼고 세므로 그대로 두면 분포의 합이 회원 수를 넘어선다.
     *
     * <p>게스트도 이 조인이 걸러 낸다. {@code player.user_id} 가 없어 회원과 이을 수 없다.
     * 화면이 세는 것은 회원이므로 맞는 동작이고, 그래서 이 목록의 합은 참여자 수보다 작다.
     *
     * <p>{@code RESULT} 는 왼쪽 조인이다. 한 판도 안 끝낸 회원이 빠지면 참여도 분포의
     * "0회" 칸이 비어 버린다. 방에 들어오기만 한 사람도 세어야 한다.
     */
    @Override
    public List<UserPlayAggregate> aggregatePlays() {
        return queryFactory
                .select(Projections.constructor(
                        UserPlayAggregate.class, PLAYER.userId, RESULT.id.count(), PLAYER.createdAt.max()))
                .from(PLAYER)
                .join(USER)
                .on(USER.id.eq(PLAYER.userId))
                .leftJoin(RESULT)
                .on(RESULT.playerId.eq(PLAYER.id))
                .groupBy(PLAYER.userId)
                .fetch();
    }

    private static ConstructorExpression<UserSummary> summaryProjection() {
        return Projections.constructor(UserSummary.class, USER.id, USER.userCode, USER.nickname, USER.createdAt);
    }

    /**
     * 닉네임 부분 일치 또는 유저코드 완전 일치.
     *
     * <p>유저코드는 완전 일치만 본다. 5자짜리 코드에 부분 일치를 걸면 아무 두 글자로도
     * 수십 명이 걸려 나와 검색이 쓸모없어진다.
     */
    private static BooleanExpression keywordMatches(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        final String trimmed = keyword.trim();
        return USER.nickname.contains(trimmed).or(USER.userCode.eq(trimmed.toUpperCase()));
    }

    private static long nullToZero(Long value) {
        return value == null ? 0L : value;
    }
}
