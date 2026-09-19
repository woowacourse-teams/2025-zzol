package coffeeshout.minigame.infra.persistence;

import static coffeeshout.minigame.domain.MiniGameType.BLIND_TIMER;
import static coffeeshout.minigame.domain.MiniGameType.BLOCK_STACKING;
import static coffeeshout.minigame.domain.MiniGameType.RACING_GAME;
import static coffeeshout.minigame.domain.MiniGameType.SPEED_TOUCH;

import coffeeshout.gamecommon.MemberMiniGameRecordQuery;
import coffeeshout.minigame.domain.MiniGameType;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 회원의 미니게임 기록을 {@code mini_game_result.user_id}로 집계하는 {@link MemberMiniGameRecordQuery} 구현체(#1794).
 */
@Component
@RequiredArgsConstructor
public class MemberMiniGameRecordQueryAdapter implements MemberMiniGameRecordQuery {

    private static final QMiniGameResultEntity RESULT = QMiniGameResultEntity.miniGameResultEntity;

    /** 기록 화면에 올리는 네 게임과 그 순서. */
    private static final List<MiniGameType> RECORD_TYPES =
            List.of(RACING_GAME, BLOCK_STACKING, BLIND_TIMER, SPEED_TOUCH);

    /**
     * 완주 기록 경계. 1 to 25·초시계는 DNF·타임아웃을 10^6 이상 값으로 저장하므로 그 미만만 완주로 본다.
     * 이 두 게임은 {@code :admin} 대시보드 TOP 집계와 같은 기준이다. 레이싱은 완주 ms만 저장하고 DNF 값이 없어
     * 경계가 안전장치로만 걸린다. 블록 쌓기는 층수라 제외가 없다.
     */
    private static final long FINISH_SCORE_CEILING = 1_000_000L;

    /** 네 게임의 완주 행만 고르는 조건. 회원 본인 집계와 전체 회원 집계가 같은 경계를 쓴다. */
    private static final BooleanExpression COMPLETED = RESULT.miniGameType
            .in(RECORD_TYPES)
            .and(RESULT.miniGameType.eq(BLOCK_STACKING).or(RESULT.score.lt(FINISH_SCORE_CEILING)));

    private final JPAQueryFactory queryFactory;

    @Override
    public MiniGameRecords findByUserId(long userId) {
        final Map<MiniGameType, Long> playCounts = queryFactory
                .select(RESULT.miniGameType, RESULT.count())
                .from(RESULT)
                .where(RESULT.userId.eq(userId))
                .groupBy(RESULT.miniGameType)
                .fetch()
                .stream()
                .collect(Collectors.toMap(row -> row.get(RESULT.miniGameType), row -> row.get(RESULT.count())));

        final Map<MiniGameType, Tuple> completed = queryFactory
                .select(RESULT.miniGameType, RESULT.count(), RESULT.score.min(), RESULT.score.max(), RESULT.score.avg())
                .from(RESULT)
                .where(RESULT.userId.eq(userId), COMPLETED)
                .groupBy(RESULT.miniGameType)
                .fetch()
                .stream()
                .collect(Collectors.toMap(row -> row.get(RESULT.miniGameType), Function.identity()));

        // ponytail: 회원 수만큼 행을 읽는다. 회원이 수만 명이 되면 윈도 함수로 바꾼다.
        final Map<MiniGameType, List<Tuple>> members = queryFactory
                .select(RESULT.miniGameType, RESULT.userId, RESULT.count(), RESULT.score.sumLong(), RESULT.score.avg())
                .from(RESULT)
                .where(RESULT.userId.isNotNull(), COMPLETED)
                .groupBy(RESULT.miniGameType, RESULT.userId)
                .fetch()
                .stream()
                .collect(Collectors.groupingBy(row -> row.get(RESULT.miniGameType)));

        final List<GameRecord> games = RECORD_TYPES.stream()
                .map(type -> toGameRecord(type, completed.get(type), members.getOrDefault(type, List.of())))
                .toList();
        final int totalPlayCount =
                playCounts.values().stream().mapToInt(Long::intValue).sum();
        return new MiniGameRecords(totalPlayCount, mostPlayed(playCounts), games);
    }

    /** 최다 게임. 동률이면 enum 순서가 앞선 게임이다. 판이 없으면 null. */
    private static MostPlayed mostPlayed(Map<MiniGameType, Long> playCounts) {
        MostPlayed mostPlayed = null;
        for (MiniGameType type : MiniGameType.values()) {
            final long count = playCounts.getOrDefault(type, 0L);
            if (count > 0 && (mostPlayed == null || count > mostPlayed.playCount())) {
                mostPlayed = new MostPlayed(type, (int) count);
            }
        }
        return mostPlayed;
    }

    /**
     * @param mine    회원 본인의 완주 집계. 없으면 null
     * @param members 완주 기록이 있는 회원별 집계 행. 전체 평균·회원 수·상위 %의 모집단
     */
    private static GameRecord toGameRecord(MiniGameType type, Tuple mine, List<Tuple> members) {
        final Long globalAverage = globalAverage(members);
        final int memberCount = members.size();
        if (mine == null) {
            return new GameRecord(type, 0, null, null, globalAverage, null, memberCount);
        }
        final Long best = type == BLOCK_STACKING ? mine.get(RESULT.score.max()) : mine.get(RESULT.score.min());
        final double myAverage = mine.get(RESULT.score.avg());
        return new GameRecord(
                type,
                mine.get(RESULT.count()).intValue(),
                best,
                Math.round(myAverage),
                globalAverage,
                percentile(type, myAverage, members),
                memberCount);
    }

    /** 판수 가중 전체 평균. 회원 완주 기록이 없으면 null. */
    private static Long globalAverage(List<Tuple> members) {
        if (members.isEmpty()) {
            return null;
        }
        long sum = 0;
        long count = 0;
        for (Tuple row : members) {
            sum += row.get(RESULT.score.sumLong());
            count += row.get(RESULT.count());
        }
        return Math.round((double) sum / count);
    }

    /** 내 평균이 회원별 평균 중 몇 등인지를 상위 %로. 동률은 같은 등수라 나보다 엄격히 좋은 회원만 센다. */
    private static int percentile(MiniGameType type, double myAverage, List<Tuple> members) {
        long betterCount = 0;
        for (Tuple row : members) {
            final double average = row.get(RESULT.score.avg());
            final boolean better = type == BLOCK_STACKING ? average > myAverage : average < myAverage;
            if (better) {
                betterCount++;
            }
        }
        return (int) Math.ceil((betterCount + 1) * 100.0 / members.size());
    }
}
