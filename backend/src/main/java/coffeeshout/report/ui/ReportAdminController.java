package coffeeshout.report.ui;

import coffeeshout.report.application.ReportAdminService;
import coffeeshout.report.application.ReportAdminService.ReportRow;
import coffeeshout.report.domain.ReportStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
public class ReportAdminController {

    private final ReportAdminService reportAdminService;

    @GetMapping
    public String dashboard(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        final Page<ReportRow> reports = reportAdminService.list(status, page);

        if (!reports.isEmpty() || reports.getTotalPages() == 0) {
            model.addAttribute("reports", reports);
            model.addAttribute("statusFilter", status);
            model.addAttribute("pendingCount", reportAdminService.countPending());
            model.addAttribute("currentPage", page);
            model.addAttribute("pageStart", Math.max(0, page - 2));
            model.addAttribute("pageEnd", Math.min(reports.getTotalPages() - 1, page + 2));
            return "admin/report";
        }

        return "redirect:/admin/reports?status=" + (status != null ? status : "") + "&page=" + (reports.getTotalPages() - 1);
    }

    @PostMapping("/{id}/resolve")
    public String resolve(
            @PathVariable Long id,
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "0") int page
    ) {
        reportAdminService.resolve(id);
        final String statusParam = status != null ? "&status=" + status : "";
        return "redirect:/admin/reports?page=" + page + statusParam;
    }
}
