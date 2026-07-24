package coffeeshout.settlement.application;

import coffeeshout.settlement.application.SettlementService.SettledScore;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
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

    public static String leaderboardKey(String seasonKey) {
        return KEY_PREFIX + seasonKey;
    }
}
