package coffeeshout.report.infra.persistence;

import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.report.domain.ReportCategory;
import coffeeshout.report.domain.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReportAdminQueryRepository {

    Page<Report> findWithFilters(
            ReportStatus status,
            ReportCategory category,
            MiniGameType gameType,
            Pageable pageable
    );
}
