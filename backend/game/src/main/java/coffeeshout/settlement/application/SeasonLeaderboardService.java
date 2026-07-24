package coffeeshout.settlement.application;

import coffeeshout.settlement.application.SettlementService.SettledScore;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Service;

/**
 * 시즌 리더보드(Redis ZSET). 진실 공급원은 season_score 테이블이고, ZSET은 순위 조회를
 * O(logN)으로 만드는 파생 뷰다.
 * <p>
 * 갱신은 증분(ZINCRBY)이 아니라 <b>절대값(ZADD)</b>이다. 컨슈머 재전달로 같은 정산 결과가
 * 두 번 반영되어도 누적 절대값을 다시 쓰는 것이라 점수가 부풀지 않는다(#1610).
 */
@Service
@RequiredArgsConstructor
public class SeasonLeaderboardService {

    private static final String KEY_PREFIX = "settlement:leaderboard:";
    // 월 시즌 종료 후에도 잠시 조회할 수 있게 여유를 두고, 옛 시즌 ZSET은 자동 소멸시킨다
    private static final Duration RETENTION = Duration.ofDays(40);

    private final StringRedisTemplate stringRedisTemplate;

    public void updateScore(SettledScore settled) {
        final String key = leaderboardKey(settled.seasonKey());
        stringRedisTemplate.opsForZSet().add(key, String.valueOf(settled.userId()), settled.totalPoints());
        stringRedisTemplate.expire(key, RETENTION);
    }

    /** 시즌 상위 {@code limit}명 (포인트 내림차순). */
    public List<LeaderboardEntry> top(String seasonKey, int limit) {
        final Set<TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(leaderboardKey(seasonKey), 0, limit - 1L);
        if (tuples == null) {
            return List.of();
        }
        final List<LeaderboardEntry> entries = new ArrayList<>();
        int rank = 1;
        for (TypedTuple<String> tuple : tuples) {
            if (tuple.getValue() == null || tuple.getScore() == null) {
                continue;
            }
            entries.add(new LeaderboardEntry(Long.parseLong(tuple.getValue()), rank++, tuple.getScore().longValue()));
        }
        return entries;
    }

    /** 특정 회원의 시즌 순위. 이번 시즌 정산 이력이 없으면 빈 값. */
    public Optional<LeaderboardEntry> rankOf(String seasonKey, long userId) {
        final String key = leaderboardKey(seasonKey);
        final String member = String.valueOf(userId);
        final Long rank = stringRedisTemplate.opsForZSet().reverseRank(key, member);
        final Double score = stringRedisTemplate.opsForZSet().score(key, member);
        if (rank == null || score == null) {
            return Optional.empty();
        }
        return Optional.of(new LeaderboardEntry(userId, (int) (rank + 1), score.longValue()));
    }

    /** 시즌에 정산 이력이 있는 회원 수. */
    public long memberCount(String seasonKey) {
        final Long size = stringRedisTemplate.opsForZSet().zCard(leaderboardKey(seasonKey));
        return size == null ? 0 : size;
    }

    public static String leaderboardKey(String seasonKey) {
        return KEY_PREFIX + seasonKey;
    }

    public record LeaderboardEntry(long userId, int rank, long totalPoints) {
    }
}
