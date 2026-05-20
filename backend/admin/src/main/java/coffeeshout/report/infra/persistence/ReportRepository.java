package coffeeshout.report.infra.persistence;

import coffeeshout.report.domain.ReportAnonymizationRepository;
import coffeeshout.report.domain.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ReportRepository extends JpaRepository<Report, Long>, ReportAdminQueryRepository,
        ReportAnonymizationRepository {

    long countByStatus(ReportStatus status);

    @Override
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Report r SET r.author.userCode = null WHERE r.author.userId = :userId")
    void clearUserCodeByUserId(@Param("userId") Long userId);
}
