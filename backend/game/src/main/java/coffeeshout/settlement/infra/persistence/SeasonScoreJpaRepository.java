package coffeeshout.settlement.infra.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeasonScoreJpaRepository extends JpaRepository<SeasonScoreEntity, Long> {

    Optional<SeasonScoreEntity> findBySeasonKeyAndUserId(String seasonKey, Long userId);

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
