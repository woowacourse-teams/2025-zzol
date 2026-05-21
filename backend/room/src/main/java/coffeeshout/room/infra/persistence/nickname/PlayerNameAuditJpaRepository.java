package coffeeshout.room.infra.persistence.nickname;

import coffeeshout.room.application.port.PlayerNameAuditRepository;
import coffeeshout.room.domain.audit.PlayerNameAuditStatus;
import java.util.Set;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface PlayerNameAuditJpaRepository extends Repository<PlayerNameAuditEntity, Long>, PlayerNameAuditRepository {

    @Override
    @Query("SELECT DISTINCT n.playerName FROM PlayerNameAuditEntity n WHERE n.status = :status")
    Set<String> findPlayerNamesByStatus(@Param("status") PlayerNameAuditStatus status);
}
