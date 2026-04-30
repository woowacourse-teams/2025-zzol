package coffeeshout.report.application;

import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.report.application.event.ReportSubmittedEvent;
import coffeeshout.report.infra.persistence.Report;
import coffeeshout.report.domain.ReportCategory;
import coffeeshout.report.infra.persistence.ReportRepository;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Transactional
    public long submit(ReportCategory category, MiniGameType gameType, String joinCode, String content) {
        return submit(category, gameType, joinCode, content, null, null);
    }

    @Transactional
    public long submit(ReportCategory category, MiniGameType gameType, String joinCode, String content,
                       Long userId, String userCode) {
        final Report entity = category == ReportCategory.BUG
                ? Report.createBugReport(gameType, joinCode, content, clock, userId, userCode)
                : Report.createGeneralReport(category, content, clock, userId, userCode);
        final Report saved = reportRepository.save(entity);
        eventPublisher.publishEvent(
                new ReportSubmittedEvent(
                        saved.getId(),
                        saved.getCategory(),
                        saved.getGameType(),
                        saved.getJoinCode(),
                        saved.getContent()
                ));
        return saved.getId();
    }
}

