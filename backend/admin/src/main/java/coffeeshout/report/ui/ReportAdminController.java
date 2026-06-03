package coffeeshout.report.ui;

import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.report.application.ReportAdminService;
import coffeeshout.report.application.ReportAdminService.ReportRow;
import coffeeshout.report.domain.ReportCategory;
import coffeeshout.report.domain.ReportStatus;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/reports")
@Validated
@RequiredArgsConstructor
public class ReportAdminController {

    private final ReportAdminService reportAdminService;

    @GetMapping
    public String dashboard(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) ReportCategory category,
            @RequestParam(required = false) MiniGameType gameType,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            Model model
    ) {
        final Page<ReportRow> reports = reportAdminService.list(status, category, gameType, page);

        if (!reports.isEmpty() || reports.getTotalPages() == 0) {
            model.addAttribute("reports", reports);
            model.addAttribute("statusFilter", status);
            model.addAttribute("categoryFilter", category);
            model.addAttribute("gameTypeFilter", gameType);
            model.addAttribute("categories", ReportCategory.values());
            model.addAttribute("gameTypes", MiniGameType.values());
            model.addAttribute("pendingCount", reportAdminService.countPending());
            model.addAttribute("currentPage", page);
            model.addAttribute("pageStart", Math.max(0, page - 2));
            model.addAttribute("pageEnd", Math.min(reports.getTotalPages() - 1, page + 2));
            return "admin/report";
        }

        return buildRedirect(reports.getTotalPages() - 1, status, category, gameType);
    }

    @PostMapping("/{id}/resolve")
    public String resolve(
            @PathVariable Long id,
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) ReportCategory category,
            @RequestParam(required = false) MiniGameType gameType,
            @RequestParam(defaultValue = "0") int page
    ) {
        reportAdminService.resolve(id);
        return buildRedirect(page, status, category, gameType);
    }

    @PostMapping("/{id}/unblock-reporter-ip")
    public String unblockReporterIp(
            @PathVariable Long id,
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) ReportCategory category,
            @RequestParam(required = false) MiniGameType gameType,
            @RequestParam(defaultValue = "0") int page
    ) {
        reportAdminService.unblockReporterIp(id);
        return buildRedirect(page, status, category, gameType);
    }

    private String buildRedirect(int page, ReportStatus status, ReportCategory category, MiniGameType gameType) {
        final StringBuilder url = new StringBuilder("redirect:/admin/reports?page=").append(page);
        if (status != null) url.append("&status=").append(status);
        if (category != null) url.append("&category=").append(category);
        if (gameType != null) url.append("&gameType=").append(gameType);
        return url.toString();
    }
}
