package coffeeshout.report.application;

import coffeeshout.global.exception.GlobalErrorCode;
import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.report.infra.persistence.Report;
import coffeeshout.report.domain.ReportCategory;
import coffeeshout.report.domain.ReportStatus;
import coffeeshout.report.infra.persistence.ReportRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportAdminService {

    private static final int PAGE_SIZE = 20;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ReportRepository reportRepository;

    @Transactional(readOnly = true)
    public Page<ReportRow> list(ReportStatus status, ReportCategory category, MiniGameType gameType, int page) {
        return reportRepository
                .findWithFilters(status, category, gameType, PageRequest.of(page, PAGE_SIZE))
                .map(this::toRow);
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return reportRepository.countByStatus(ReportStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public String findReporterIp(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_EXIST, "신고를 찾을 수 없습니다."))
                .getIp();
    }

    @Transactional
    public void resolve(Long id) {
        final Report report = reportRepository.findById(id)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_EXIST, "신고를 찾을 수 없습니다."));

        if (report.getStatus() == ReportStatus.RESOLVED) {
            return;
        }
        report.resolve();
    }

    private ReportRow toRow(Report e) {
        return new ReportRow(
                e.getId(),
                e.getCategory(),
                e.getGameType(),
                e.getJoinCode(),
                e.getContent(),
                e.getStatus(),
                toKst(e.getCreatedAt()),
                e.getResolvedAt() != null ? toKst(e.getResolvedAt()) : null,
                e.getIp()
        );
    }

    private LocalDateTime toKst(Instant instant) {
        return LocalDateTime.ofInstant(instant, KST);
    }

    public record ReportRow(
            Long id,
            ReportCategory category,
            MiniGameType gameType,
            String joinCode,
            String content,
            ReportStatus status,
            LocalDateTime createdAt,
            LocalDateTime resolvedAt,
            String ip
    ) {
    }
}
