package coffeeshout.report.ui.response;

import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.report.application.ReportAdminService.ReportRow;
import coffeeshout.report.domain.ReportCategory;
import coffeeshout.report.domain.ReportStatus;
import java.time.LocalDateTime;

/**
 * @param createdAt KST 기준. 서비스 계층이 이미 변환해 넘긴다.
 */
public record ReportResponse(
        Long id,
        ReportCategory category,
        MiniGameType gameType,
        String joinCode,
        String content,
        ReportStatus status,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt,
        String ip) {

    public static ReportResponse from(ReportRow row) {
        return new ReportResponse(
                row.id(),
                row.category(),
                row.gameType(),
                row.joinCode(),
                row.content(),
                row.status(),
                row.createdAt(),
                row.resolvedAt(),
                row.ip());
    }
}
