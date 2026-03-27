package coffeeshout.room.ui;

import coffeeshout.room.application.service.nickname.PlayerNameAuditService;
import coffeeshout.room.application.service.nickname.PlayerNameFeedbackService;
import coffeeshout.room.domain.audit.AiConfidence;
import coffeeshout.room.domain.audit.PlayerNameAuditStatus;
import coffeeshout.room.infra.persistence.nickname.PlayerNameAuditEntity;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/playername-audit")
@RequiredArgsConstructor
public class PlayerNameAuditAdminController {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int PAGE_SIZE = 10;
    private static final Sort AUDITED_AT_DESC = Sort.by("auditedAt").descending();

    private final PlayerNameAuditService playerNameAuditService;
    private final PlayerNameFeedbackService playerNameFeedbackService;

    @GetMapping
    public String dashboard(
            @RequestParam(defaultValue = "0") int flaggedPage,
            @RequestParam(defaultValue = "0") int pendingPage,
            Model model) {

        final Page<AuditRow> flagged = playerNameAuditService
                .listByStatus(PlayerNameAuditStatus.FLAGGED, PageRequest.of(flaggedPage, PAGE_SIZE, AUDITED_AT_DESC))
                .map(this::toRow);

        if (flagged.isEmpty() && flagged.getTotalPages() > 0) {
            return redirectTo(flagged.getTotalPages() - 1, pendingPage);
        }

        final Page<AuditRow> pending = playerNameAuditService
                .listByStatus(PlayerNameAuditStatus.PENDING, PageRequest.of(pendingPage, PAGE_SIZE, AUDITED_AT_DESC))
                .map(this::toRow);

        if (pending.isEmpty() && pending.getTotalPages() > 0) {
            return redirectTo(flaggedPage, pending.getTotalPages() - 1);
        }

        model.addAttribute("flagged", flagged);
        model.addAttribute("pending", pending);
        model.addAttribute("flaggedPage", flaggedPage);
        model.addAttribute("pendingPage", pendingPage);
        model.addAttribute("flaggedPageStart", Math.max(0, flaggedPage - 2));
        model.addAttribute("flaggedPageEnd", Math.min(flagged.getTotalPages() - 1, flaggedPage + 2));
        model.addAttribute("pendingPageStart", Math.max(0, pendingPage - 2));
        model.addAttribute("pendingPageEnd", Math.min(pending.getTotalPages() - 1, pendingPage + 2));
        return "admin/nickname-audit";
    }

    @PostMapping("/{id}/allow")
    public String allow(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int flaggedPage,
            @RequestParam(defaultValue = "0") int pendingPage) {
        playerNameFeedbackService.allow(id);
        return redirectTo(flaggedPage, pendingPage);
    }

    @PostMapping("/{id}/block")
    public String block(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int flaggedPage,
            @RequestParam(defaultValue = "0") int pendingPage) {
        playerNameFeedbackService.block(id);
        return redirectTo(flaggedPage, pendingPage);
    }

    private String redirectTo(int flaggedPage, int pendingPage) {
        return "redirect:/admin/playername-audit?flaggedPage=" + flaggedPage + "&pendingPage=" + pendingPage;
    }

    private AuditRow toRow(PlayerNameAuditEntity e) {
        return new AuditRow(
                e.getId(),
                e.getPlayerName(),
                e.getConfidence() != null ? e.getConfidence() : AiConfidence.UNKNOWN,
                e.getReason() != null ? e.getReason() : "",
                LocalDateTime.ofInstant(e.getCreatedAt(), KST),
                e.getAuditedAt() != null ? LocalDateTime.ofInstant(e.getAuditedAt(), KST) : null
        );
    }

    public record AuditRow(
            Long id,
            String playerName,
            AiConfidence confidence,
            String reason,
            LocalDateTime createdAt,
            LocalDateTime auditedAt
    ) {
    }
}
