package coffeeshout.profanity.application.port;

import coffeeshout.profanity.domain.audit.NicknameAudit;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NicknameAuditRepository {

    NicknameAudit save(NicknameAudit entity);

    void insertUnaudited(String nickname, Instant createdAt);

    List<NicknameAudit> saveAll(Iterable<NicknameAudit> entities);

    void deleteAll(Iterable<NicknameAudit> entities);

    Optional<NicknameAudit> findById(Long id);

    long countByStatusAndAuditedAtIsNull(NicknameAuditStatus status);

    long countByStatus(NicknameAuditStatus status);

    List<NicknameAudit> findByStatusAndAuditedAtIsNull(NicknameAuditStatus status, Pageable pageable);

    Page<NicknameAudit> findByStatus(NicknameAuditStatus status, Pageable pageable);

    Set<String> findNicknamesByStatus(NicknameAuditStatus status);

    boolean existsByNickname(String nickname);

    boolean existsByNicknameAndStatus(String nickname, NicknameAuditStatus status);

    /**
     * 주어진 닉네임들 중 이미 검열 완료된(UNAUDITED가 아닌 terminal) 행을 가진 닉네임 집합을 반환한다.
     * 배치 승격 시 잔존 중복 UNAUDITED를 건별 조회 없이 한 번에 판별하기 위한 벌크 조회다.
     */
    Set<String> findNicknamesWithTerminalStatus(Collection<String> nicknames);

    /**
     * 검열 호출이 실패한 배치 행들의 시도 횟수를 한 번에 올린다.
     *
     * <p>세지 않으면 파싱 실패를 일으키는 닉네임 하나가 회차마다 같은 실패를 되풀이한다.
     */
    void incrementAttemptCount(Collection<Long> ids);

    /**
     * 시도 횟수가 상한에 닿은 행을 DEAD_LETTER로 내린다. DEAD_LETTER는 UNAUDITED가 아니므로
     * 스캔 쿼리에서 저절로 빠진다.
     *
     * @return DEAD_LETTER로 내려간 행 수
     */
    int markDeadLetterAtAttemptLimit(Collection<Long> ids, int maxAttempts);
}
