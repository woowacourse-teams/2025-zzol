package coffeeshout.profanity.infra.persistence.audit;

import coffeeshout.profanity.application.port.NicknameAuditRepository;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import java.util.Set;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface NicknameAuditJpaRepository extends Repository<NicknameAuditEntity, Long>, NicknameAuditRepository {

    @Override
    @Query("SELECT DISTINCT n.nickname FROM NicknameAuditEntity n WHERE n.status = :status")
    Set<String> findNicknamesByStatus(@Param("status") NicknameAuditStatus status);
}
