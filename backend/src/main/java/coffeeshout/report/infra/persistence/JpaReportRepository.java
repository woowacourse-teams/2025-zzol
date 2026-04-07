package coffeeshout.report.infra.persistence;

import coffeeshout.report.domain.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaReportRepository extends JpaRepository<ReportEntity, Long> {

    Page<ReportEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<ReportEntity> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    long countByStatus(ReportStatus status);
}
