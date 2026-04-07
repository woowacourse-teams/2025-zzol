package coffeeshout.report.application;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.global.ratelimit.ReportRateLimitStore;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.report.application.event.ReportSubmittedEvent;
import coffeeshout.report.domain.ReportCategory;
import coffeeshout.report.exception.ReportErrorCode;
import coffeeshout.report.infra.persistence.ReportEntity;
import coffeeshout.report.domain.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ReportRateLimitStore rateLimitStore;

    @Transactional
    public long submit(String ip, ReportCategory category, MiniGameType gameType, String joinCode, String content) {
        if (!rateLimitStore.tryAcquire(ip)) {
            throw new BusinessException(ReportErrorCode.REPORT_RATE_LIMITED, ReportErrorCode.REPORT_RATE_LIMITED.getMessage());
        }
        final ReportEntity saved = reportRepository.save(
                ReportEntity.create(category, gameType, joinCode, content)
        );
        eventPublisher.publishEvent(
                new ReportSubmittedEvent(saved.getId(), category, gameType, joinCode, content)
        );
        return saved.getId();
    }
}

