package coffeeshout.minigame.infra.persistence;

import static coffeeshout.minigame.domain.MiniGameType.BLIND_TIMER;
import static coffeeshout.minigame.domain.MiniGameType.BLOCK_STACKING;
import static coffeeshout.minigame.domain.MiniGameType.RACING_GAME;
import static coffeeshout.minigame.domain.MiniGameType.SPEED_TOUCH;

import coffeeshout.gamecommon.MemberMiniGameRecordQuery;
import coffeeshout.minigame.domain.MiniGameType;
import com.querydsl.core.Tuple;
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
                .where(
                        RESULT.userId.eq(userId),
                        RESULT.miniGameType.in(RECORD_TYPES),
                        RESULT.miniGameType.eq(BLOCK_STACKING).or(RESULT.score.lt(FINISH_SCORE_CEILING)))
                .groupBy(RESULT.miniGameType)
                .fetch()
                .stream()
                .collect(Collectors.toMap(row -> row.get(RESULT.miniGameType), Function.identity()));

        final List<GameRecord> games = RECORD_TYPES.stream()
                .map(type -> toGameRecord(type, completed.get(type)))
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

    private static GameRecord toGameRecord(MiniGameType type, Tuple row) {
        if (row == null) {
            return new GameRecord(type, 0, null, null);
        }
        final Long best = type == BLOCK_STACKING ? row.get(RESULT.score.max()) : row.get(RESULT.score.min());
        return new GameRecord(type, row.get(RESULT.count()).intValue(), best, Math.round(row.get(RESULT.score.avg())));
    }
}
