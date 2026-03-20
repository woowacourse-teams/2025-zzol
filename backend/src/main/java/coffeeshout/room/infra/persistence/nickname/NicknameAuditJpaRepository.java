package coffeeshout.room.infra.persistence.nickname;

import coffeeshout.room.domain.audit.NicknameAuditStatus;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface NicknameAuditJpaRepository extends Repository<NicknameAuditEntity, Long> {

    NicknameAuditEntity save(NicknameAuditEntity entity);

    List<NicknameAuditEntity> saveAll(Iterable<NicknameAuditEntity> entities);

    Optional<NicknameAuditEntity> findById(Long id);

    long countByStatusAndAuditedAtIsNull(NicknameAuditStatus status);

    long countByStatus(NicknameAuditStatus status);

    List<NicknameAuditEntity> findByStatusAndAuditedAtIsNull(NicknameAuditStatus status, Pageable pageable);

    Page<NicknameAuditEntity> findByStatus(NicknameAuditStatus status, Pageable pageable);

    @Query("SELECT n.nickname FROM NicknameAuditEntity n WHERE n.status = :status")
    Set<String> findNicknamesByStatus(@Param("status") NicknameAuditStatus status);
}
