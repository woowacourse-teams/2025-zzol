package coffeeshout.profanity.infra.persistence.audit;

import coffeeshout.profanity.application.port.NicknameAuditRepository;
import coffeeshout.profanity.domain.audit.NicknameAudit;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface NicknameAuditJpaRepository extends Repository<NicknameAudit, Long>, NicknameAuditRepository {

    @Override
    @Modifying
    // attempt_count를 명시한다. 마이그레이션은 DEFAULT 0을 주지만, ddl-auto로 스키마를 만드는
    // 테스트·로컬에는 기본값이 없어 이 INSERT가 통째로 실패한다.
    @Query(
            value = "INSERT INTO player_name_audit (player_name, status, attempt_count, created_at) "
                    + "VALUES (:nickname, 'UNAUDITED', 0, :createdAt) "
                    + "ON DUPLICATE KEY UPDATE id = id",
            nativeQuery = true)
    void insertUnaudited(@Param("nickname") String nickname, @Param("createdAt") Instant createdAt);

    @Override
    @Query("SELECT DISTINCT n.nickname FROM NicknameAudit n WHERE n.status = :status")
    Set<String> findNicknamesByStatus(@Param("status") NicknameAuditStatus status);

    @Override
    @Query("SELECT DISTINCT n.nickname FROM NicknameAudit n "
            + "WHERE n.nickname IN :nicknames "
            + "AND n.status <> coffeeshout.profanity.domain.audit.NicknameAuditStatus.UNAUDITED")
    Set<String> findNicknamesWithTerminalStatus(@Param("nicknames") Collection<String> nicknames);

    /**
     * 증가와 상한 판정을 한 UPDATE에 합치지 않는다. MySQL은 SET 대입을 왼쪽부터 차례로 평가해서
     * 뒤에 오는 식이 이미 증가한 값을 본다. 그러면 상한 3에 두 번째 실패부터 DEAD_LETTER가 된다.
     * 두 문장은 호출자 트랜잭션 안에서 함께 커밋되므로 중간 상태가 밖으로 보이지 않는다.
     */
    @Override
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE NicknameAudit n SET n.attemptCount = n.attemptCount + 1 "
            + "WHERE n.id IN :ids "
            + "AND n.status = coffeeshout.profanity.domain.audit.NicknameAuditStatus.UNAUDITED")
    int incrementAttemptCount(@Param("ids") Collection<Long> ids);

    @Override
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE NicknameAudit n "
            + "SET n.status = coffeeshout.profanity.domain.audit.NicknameAuditStatus.DEAD_LETTER "
            + "WHERE n.id IN :ids "
            + "AND n.attemptCount >= :maxAttempts "
            + "AND n.status = coffeeshout.profanity.domain.audit.NicknameAuditStatus.UNAUDITED")
    int markDeadLetterAtAttemptLimit(@Param("ids") Collection<Long> ids, @Param("maxAttempts") int maxAttempts);
}
