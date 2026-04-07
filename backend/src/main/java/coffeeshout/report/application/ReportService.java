package coffeeshout.report.application;

import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.report.application.event.ReportSubmittedEvent;
import coffeeshout.report.domain.ReportCategory;
import coffeeshout.report.infra.persistence.JpaReportRepository;
import coffeeshout.report.infra.persistence.ReportEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final JpaReportRepository jpaReportRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public long submit(ReportCategory category, MiniGameType gameType, String joinCode, String content) {
        final ReportEntity saved = jpaReportRepository.save(
                ReportEntity.create(category, gameType, joinCode, content)
        );
        eventPublisher.publishEvent(
                new ReportSubmittedEvent(saved.getId(), category, gameType, joinCode, content)
        );
        return saved.getId();
    }
}
