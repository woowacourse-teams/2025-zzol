package coffeeshout.report.ui;

import coffeeshout.admin.support.PageResponse;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.report.application.ReportAdminService;
import coffeeshout.report.domain.ReportCategory;
import coffeeshout.report.domain.ReportStatus;
import coffeeshout.report.ui.response.ReportResponse;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 신고 관리. 기존 Thymeleaf {@code ReportAdminController}의 REST 판이다.
 *
 * <p>필터 상태를 리다이렉트 URL 로 실어 나르던 부분이 통째로 사라졌다. SPA 가 URL 쿼리로
 * 상태를 들고 있으므로 서버가 되돌려 줄 필요가 없다.
 */
@RestController
@RequestMapping("/admin/api/reports")
@Validated
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportAdminService reportAdminService;

    @GetMapping
    public PageResponse<ReportResponse> list(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) ReportCategory category,
            @RequestParam(required = false) MiniGameType gameType,
            @RequestParam(defaultValue = "0") @Min(0) int page) {
        return PageResponse.of(reportAdminService.list(status, category, gameType, page), ReportResponse::from);
    }

    /** 홈 대시보드의 처리 대기 큐가 쓴다. 목록을 받아 세지 않아도 되게 따로 둔다. */
    @GetMapping("/pending-count")
    public long pendingCount() {
        return reportAdminService.countPending();
    }

    @PostMapping("/{id}/resolve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resolve(@PathVariable Long id) {
        reportAdminService.resolve(id);
    }

    @DeleteMapping("/{id}/reporter-ip-block")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unblockReporterIp(@PathVariable Long id) {
        reportAdminService.unblockReporterIp(id);
    }
}
