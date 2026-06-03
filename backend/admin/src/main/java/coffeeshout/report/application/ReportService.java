package coffeeshout.report.application;

import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.report.application.event.ReportSubmittedEvent;
import coffeeshout.report.domain.ReportCategory;
import coffeeshout.report.infra.persistence.Report;
import coffeeshout.report.infra.persistence.Reporter;
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
                       Reporter author) {
        return submit(category, gameType, joinCode, content, author, null);
    }

    @Transactional
    public long submit(ReportCategory category, MiniGameType gameType, String joinCode, String content,
                       Reporter author, String ip) {
        final Report.ReportCreation creation = category == ReportCategory.BUG
                ? Report.ReportCreation.bug(gameType, joinCode, content, author, ip)
                : Report.ReportCreation.general(category, content, author, ip);
        final Report entity = Report.create(creation, clock);
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

