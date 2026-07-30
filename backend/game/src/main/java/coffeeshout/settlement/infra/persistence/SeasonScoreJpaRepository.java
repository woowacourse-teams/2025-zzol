package coffeeshout.settlement.infra.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeasonScoreJpaRepository extends JpaRepository<SeasonScoreEntity, Long> {

    Optional<SeasonScoreEntity> findBySeasonKeyAndUserId(String seasonKey, Long userId);

    /** 리더보드 상위 N명 — 포인트 내림차순, 동점이면 먼저 정산을 시작한 회원(id 오름차순)이 앞. */
    List<SeasonScoreEntity> findBySeasonKeyOrderByTotalPointsDescIdAsc(String seasonKey, Pageable pageable);

    long countBySeasonKey(String seasonKey);

    /** 나보다 포인트가 높은 회원 수 — +1이 곧 내 순위다(동점자는 같은 순위 공유). */
    long countBySeasonKeyAndTotalPointsGreaterThan(String seasonKey, long totalPoints);

    /**
     * 포인트를 원자적으로 가산한다. read-modify-write 대신 DB 단일 UPDATE로 처리해,
     * 같은 회원의 결과 두 건이 서로 다른 컨슈머 인스턴스에서 동시에 정산되어도 가산이 유실되지 않는다.
     *
     * @return 갱신된 행 수 — 0이면 해당 시즌 첫 정산이라 INSERT가 필요하다
     */
    @Modifying
    @Query("""
            UPDATE SeasonScoreEntity s
               SET s.totalPoints = s.totalPoints + :points,
                   s.gamesPlayed = s.gamesPlayed + 1,
                   s.updatedAt = CURRENT_TIMESTAMP
             WHERE s.seasonKey = :seasonKey AND s.userId = :userId
            """)
    int addPoints(@Param("seasonKey") String seasonKey, @Param("userId") Long userId, @Param("points") int points);
}
