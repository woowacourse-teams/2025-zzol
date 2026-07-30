package coffeeshout.settlement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import coffeeshout.settlement.application.SeasonLeaderboardService.LeaderboardEntry;
import coffeeshout.settlement.infra.persistence.SeasonScoreEntity;
import coffeeshout.settlement.infra.persistence.SeasonScoreJpaRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SeasonLeaderboardServiceTest {

    private static final String SEASON = "2026-07";

    @InjectMocks
    SeasonLeaderboardService leaderboardService;

    @Mock
    SeasonScoreJpaRepository scoreRepository;

    @Test
    void 상위_목록은_포인트_내림차순으로_순위를_부여한다() {
        List<SeasonScoreEntity> scores = List.of(성적(1L, 170), 성적(2L, 100), 성적(3L, 70));
        given(scoreRepository.findBySeasonKeyOrderByTotalPointsDescIdAsc(eq(SEASON), any(Pageable.class)))
                .willReturn(scores);

        List<LeaderboardEntry> entries = leaderboardService.top(SEASON, 10);

        assertThat(entries).containsExactly(
                new LeaderboardEntry(1L, 1, 170),
                new LeaderboardEntry(2L, 2, 100),
                new LeaderboardEntry(3L, 3, 70)
        );
    }

    @Test
    void 동점자는_같은_순위를_공유하고_다음_순위는_건너뛴다() {
        List<SeasonScoreEntity> scores = List.of(성적(1L, 100), 성적(2L, 100), 성적(3L, 70));
        given(scoreRepository.findBySeasonKeyOrderByTotalPointsDescIdAsc(eq(SEASON), any(Pageable.class)))
                .willReturn(scores);

        List<LeaderboardEntry> entries = leaderboardService.top(SEASON, 10);

        assertThat(entries).containsExactly(
                new LeaderboardEntry(1L, 1, 100),
                new LeaderboardEntry(2L, 1, 100),
                new LeaderboardEntry(3L, 3, 70)
        );
    }

    @Test
    void 내_순위는_나보다_포인트가_높은_회원_수에_1을_더한_값이다() {
        SeasonScoreEntity score = 성적(5L, 70);
        given(scoreRepository.findBySeasonKeyAndUserId(SEASON, 5L)).willReturn(Optional.of(score));
        given(scoreRepository.countBySeasonKeyAndTotalPointsGreaterThan(SEASON, 70L)).willReturn(2L);

        Optional<LeaderboardEntry> entry = leaderboardService.rankOf(SEASON, 5L);

        assertThat(entry).contains(new LeaderboardEntry(5L, 3, 70));
    }

    @Test
    void 정산_이력이_없는_회원의_순위는_빈_값이다() {
        given(scoreRepository.findBySeasonKeyAndUserId(SEASON, 9L)).willReturn(Optional.empty());

        assertThat(leaderboardService.rankOf(SEASON, 9L)).isEmpty();
    }

    private SeasonScoreEntity 성적(long userId, long totalPoints) {
        SeasonScoreEntity score = mock(SeasonScoreEntity.class);
        given(score.getUserId()).willReturn(userId);
        given(score.getTotalPoints()).willReturn(totalPoints);
        return score;
    }
}
