package coffeeshout.profanity.application.port;

import coffeeshout.profanity.domain.audit.NicknameAudit;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NicknameAuditRepository {

    NicknameAudit save(NicknameAudit entity);

    List<NicknameAudit> saveAll(Iterable<NicknameAudit> entities);

    Optional<NicknameAudit> findById(Long id);

    long countByStatusAndAuditedAtIsNull(NicknameAuditStatus status);

    long countByStatus(NicknameAuditStatus status);

    List<NicknameAudit> findByStatusAndAuditedAtIsNull(NicknameAuditStatus status, Pageable pageable);

    Page<NicknameAudit> findByStatus(NicknameAuditStatus status, Pageable pageable);

    Set<String> findNicknamesByStatus(NicknameAuditStatus status);

    boolean existsByNickname(String nickname);

    boolean existsByNicknameAndStatus(String nickname, NicknameAuditStatus status);
}
