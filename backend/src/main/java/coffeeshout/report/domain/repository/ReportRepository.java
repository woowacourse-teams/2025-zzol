package coffeeshout.report.domain.repository;

import coffeeshout.report.domain.ReportStatus;
import coffeeshout.report.infra.persistence.ReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<ReportEntity, Long>, ReportAdminQueryRepository {

    long countByStatus(ReportStatus status);
}
