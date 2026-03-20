package coffeeshout.room.infra.persistence;

import coffeeshout.room.domain.audit.NicknameAuditStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

public interface NicknameAuditJpaRepository extends Repository<NicknameAuditEntity, Long> {

    NicknameAuditEntity save(NicknameAuditEntity entity);

    List<NicknameAuditEntity> saveAll(Iterable<NicknameAuditEntity> entities);

    Optional<NicknameAuditEntity> findById(Long id);

    long countByStatusAndAuditedAtIsNull(NicknameAuditStatus status);

    List<NicknameAuditEntity> findByStatusAndAuditedAtIsNull(NicknameAuditStatus status, Pageable pageable);
}
