package coffeeshout.settlement.application;

import coffeeshout.settlement.infra.persistence.SeasonScoreEntity;
import coffeeshout.settlement.infra.persistence.SeasonScoreJpaRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 시즌 리더보드 조회. 순위·포인트를 진실 공급원인 season_score 테이블에서 직접 읽는다.
 * <p>
 * 처음에는 Redis ZSET 파생 뷰로 만들었다가 코드 리뷰(#1612)에서 걷어냈다. 회원당 시즌에
 * 1행이고 조회 빈도도 낮아 SQL 정렬로 충분한 규모인데, 파생 뷰를 두면 DB와 어긋나는 창과
 * 조회 왕복이 비용으로 남기 때문이다. 순위 조회가 병목이 되는 규모(수만 회원·고빈도 조회)가
 * 되면 그때 ZSET 파생 뷰를 다시 검토한다.
 * <p>
 * 동점자는 같은 순위를 공유한다(경쟁 순위). 표시 순서는 포인트 내림차순, 같으면 먼저
 * 정산을 시작한 회원(id 오름차순)이 앞에 온다 — 갱신 타이밍에 따라 순서가 흔들리지 않게
 * 하기 위한 결정적 규칙이다.
 */
@Service
@RequiredArgsConstructor
public class SeasonLeaderboardService {

    private final SeasonScoreJpaRepository scoreRepository;

    /** 시즌 상위 {@code limit}명 (포인트 내림차순). */
    @Transactional(readOnly = true)
    public List<LeaderboardEntry> top(String seasonKey, int limit) {
        final List<SeasonScoreEntity> scores = scoreRepository
                .findBySeasonKeyOrderByTotalPointsDescIdAsc(seasonKey, PageRequest.of(0, limit));

        final List<LeaderboardEntry> entries = new ArrayList<>();
        int rank = 0;
        long previousPoints = Long.MIN_VALUE;
        for (int i = 0; i < scores.size(); i++) {
            final SeasonScoreEntity score = scores.get(i);
            if (score.getTotalPoints() != previousPoints) {
                rank = i + 1;
                previousPoints = score.getTotalPoints();
            }
            entries.add(new LeaderboardEntry(score.getUserId(), rank, score.getTotalPoints()));
        }
        return entries;
    }

    /** 특정 회원의 시즌 순위. 이번 시즌 정산 이력이 없으면 빈 값. */
    @Transactional(readOnly = true)
    public Optional<LeaderboardEntry> rankOf(String seasonKey, long userId) {
        return scoreRepository.findBySeasonKeyAndUserId(seasonKey, userId)
                .map(score -> new LeaderboardEntry(
                        userId,
                        Math.toIntExact(scoreRepository
                                .countBySeasonKeyAndTotalPointsGreaterThan(seasonKey, score.getTotalPoints()) + 1),
                        score.getTotalPoints()
                ));
    }

    /** 시즌에 정산 이력이 있는 회원 수. */
    @Transactional(readOnly = true)
    public long memberCount(String seasonKey) {
        return scoreRepository.countBySeasonKey(seasonKey);
    }

    public record LeaderboardEntry(long userId, int rank, long totalPoints) {
    }
}
