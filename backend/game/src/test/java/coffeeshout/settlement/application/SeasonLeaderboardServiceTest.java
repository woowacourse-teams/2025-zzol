package coffeeshout.settlement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import coffeeshout.settlement.application.SeasonLeaderboardService.LeaderboardEntry;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;

@ExtendWith(MockitoExtension.class)
class SeasonLeaderboardServiceTest {

    private static final String SEASON = "2026-07";

    @InjectMocks
    SeasonLeaderboardService leaderboardService;

    @Mock
    StringRedisTemplate stringRedisTemplate;
    @Mock
    ZSetOperations<String, String> zSetOperations;

    @Test
    void userId_형식이_아닌_오염된_멤버는_건너뛰고_나머지를_반환한다() {
        // 오염된 멤버 하나가 리더보드 조회 전체를 실패시키면 안 된다
        Set<TypedTuple<String>> tuples = new LinkedHashSet<>();
        tuples.add(TypedTuple.of("1", 100.0));
        tuples.add(TypedTuple.of("corrupted-member", 90.0));
        tuples.add(TypedTuple.of("2", 70.0));
        given(stringRedisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(zSetOperations.reverseRangeWithScores(SeasonLeaderboardService.leaderboardKey(SEASON), 0, 9))
                .willReturn(tuples);

        List<LeaderboardEntry> entries = leaderboardService.top(SEASON, 10);

        assertThat(entries).containsExactly(
                new LeaderboardEntry(1L, 1, 100),
                new LeaderboardEntry(2L, 2, 70)
        );
    }
}
