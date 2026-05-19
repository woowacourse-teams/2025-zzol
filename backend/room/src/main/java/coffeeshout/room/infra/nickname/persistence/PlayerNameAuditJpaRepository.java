package coffeeshout.room.infra.nickname.persistence;

import coffeeshout.room.domain.audit.PlayerNameAuditStatus;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface PlayerNameAuditJpaRepository extends Repository<PlayerNameAuditEntity, Long> {

    PlayerNameAuditEntity save(PlayerNameAuditEntity entity);

    List<PlayerNameAuditEntity> saveAll(Iterable<PlayerNameAuditEntity> entities);

    Optional<PlayerNameAuditEntity> findById(Long id);

    long countByStatusAndAuditedAtIsNull(PlayerNameAuditStatus status);

    long countByStatus(PlayerNameAuditStatus status);

    List<PlayerNameAuditEntity> findByStatusAndAuditedAtIsNull(PlayerNameAuditStatus status, Pageable pageable);

    Page<PlayerNameAuditEntity> findByStatus(PlayerNameAuditStatus status, Pageable pageable);

    @Query("SELECT DISTINCT n.playerName FROM PlayerNameAuditEntity n WHERE n.status = :status")
    Set<String> findPlayerNamesByStatus(@Param("status") PlayerNameAuditStatus status);

    boolean existsByPlayerNameAndStatus(String playerName, PlayerNameAuditStatus status);
}
