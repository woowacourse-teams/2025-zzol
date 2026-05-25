package coffeeshout.profanity.application.port;

import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import coffeeshout.profanity.infra.persistence.audit.NicknameAuditEntity;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NicknameAuditRepository {

    NicknameAuditEntity save(NicknameAuditEntity entity);

    List<NicknameAuditEntity> saveAll(Iterable<NicknameAuditEntity> entities);

    Optional<NicknameAuditEntity> findById(Long id);

    long countByStatusAndAuditedAtIsNull(NicknameAuditStatus status);

    long countByStatus(NicknameAuditStatus status);

    List<NicknameAuditEntity> findByStatusAndAuditedAtIsNull(NicknameAuditStatus status, Pageable pageable);

    Page<NicknameAuditEntity> findByStatus(NicknameAuditStatus status, Pageable pageable);

    Set<String> findNicknamesByStatus(NicknameAuditStatus status);

    boolean existsByNicknameAndStatus(String nickname, NicknameAuditStatus status);
}
