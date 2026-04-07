package coffeeshout.report.domain.repository;

import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.report.domain.ReportCategory;
import coffeeshout.report.domain.ReportStatus;
import coffeeshout.report.infra.persistence.ReportEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReportAdminQueryRepository {

    Page<ReportEntity> findWithFilters(
            ReportStatus status,
            ReportCategory category,
            MiniGameType gameType,
            Pageable pageable
    );
}
