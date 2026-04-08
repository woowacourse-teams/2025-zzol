package coffeeshout.report.infra.persistence;

import coffeeshout.report.domain.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long>, ReportAdminQueryRepository {

    long countByStatus(ReportStatus status);
}
