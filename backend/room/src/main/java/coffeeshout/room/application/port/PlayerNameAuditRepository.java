package coffeeshout.room.application.port;

import coffeeshout.room.domain.audit.PlayerNameAuditStatus;
import coffeeshout.room.infra.persistence.nickname.PlayerNameAuditEntity;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PlayerNameAuditRepository {

    PlayerNameAuditEntity save(PlayerNameAuditEntity entity);

    List<PlayerNameAuditEntity> saveAll(Iterable<PlayerNameAuditEntity> entities);

    Optional<PlayerNameAuditEntity> findById(Long id);

    long countByStatusAndAuditedAtIsNull(PlayerNameAuditStatus status);

    long countByStatus(PlayerNameAuditStatus status);

    List<PlayerNameAuditEntity> findByStatusAndAuditedAtIsNull(PlayerNameAuditStatus status, Pageable pageable);

    Page<PlayerNameAuditEntity> findByStatus(PlayerNameAuditStatus status, Pageable pageable);

    Set<String> findPlayerNamesByStatus(PlayerNameAuditStatus status);

    boolean existsByPlayerNameAndStatus(String playerName, PlayerNameAuditStatus status);
}
