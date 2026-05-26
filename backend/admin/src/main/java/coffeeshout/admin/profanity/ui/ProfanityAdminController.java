package coffeeshout.admin.profanity.ui;

import coffeeshout.admin.profanity.ui.request.AddProfanityWordRequest;
import coffeeshout.profanity.application.ProfanityAuditService;
import coffeeshout.profanity.application.ProfanityFeedbackService;
import coffeeshout.profanity.application.ProfanityWordManagementService;
import coffeeshout.profanity.domain.Language;
import coffeeshout.profanity.domain.ProfanityWord;
import coffeeshout.profanity.domain.WordSource;
import coffeeshout.profanity.domain.audit.AiConfidence;
import coffeeshout.profanity.domain.audit.NicknameAudit;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/profanity")
@RequiredArgsConstructor
public class ProfanityAdminController {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int PAGE_SIZE = 10;
    private static final int WORDS_PAGE_SIZE = 20;
    private static final Sort AUDITED_AT_DESC = Sort.by("auditedAt").descending();

    private final ProfanityAuditService auditService;
    private final ProfanityFeedbackService feedbackService;
    private final ProfanityWordManagementService managementService;

    @GetMapping
    public String page(
            @RequestParam(defaultValue = "audit") String tab,
            @RequestParam(defaultValue = "0") int flaggedPage,
            @RequestParam(defaultValue = "0") int pendingPage,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int wordsPage,
            @RequestParam(defaultValue = "") String language,
            @RequestParam(defaultValue = "") String source,
            @RequestParam(defaultValue = "") String activeFilter,
            Model model) {

        model.addAttribute("tab", tab);

        if ("words".equals(tab)) {
            populateWordsModel(model, search, language, source, activeFilter, wordsPage);
            return "admin/profanity";
        }

        final Page<AuditRow> flagged = auditService
                .listByStatus(NicknameAuditStatus.FLAGGED, PageRequest.of(flaggedPage, PAGE_SIZE, AUDITED_AT_DESC))
                .map(this::toRow);

        if (flagged.isEmpty() && flagged.getTotalPages() > 0) {
            return redirectAudit(flagged.getTotalPages() - 1, pendingPage);
        }

        final Page<AuditRow> pending = auditService
                .listByStatus(NicknameAuditStatus.PENDING, PageRequest.of(pendingPage, PAGE_SIZE, AUDITED_AT_DESC))
                .map(this::toRow);

        if (pending.isEmpty() && pending.getTotalPages() > 0) {
            return redirectAudit(flaggedPage, pending.getTotalPages() - 1);
        }

        model.addAttribute("flagged", flagged);
        model.addAttribute("pending", pending);
        model.addAttribute("flaggedPage", flaggedPage);
        model.addAttribute("pendingPage", pendingPage);
        model.addAttribute("flaggedPageStart", Math.max(0, flaggedPage - 2));
        model.addAttribute("flaggedPageEnd", Math.min(flagged.getTotalPages() - 1, flaggedPage + 2));
        model.addAttribute("pendingPageStart", Math.max(0, pendingPage - 2));
        model.addAttribute("pendingPageEnd", Math.min(pending.getTotalPages() - 1, pendingPage + 2));
        return "admin/profanity";
    }

    @PostMapping("/audit/{id}/allow")
    public String allow(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int flaggedPage,
            @RequestParam(defaultValue = "0") int pendingPage) {
        feedbackService.allow(id);
        return redirectAudit(flaggedPage, pendingPage);
    }

    @PostMapping("/audit/{id}/block")
    public String block(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int flaggedPage,
            @RequestParam(defaultValue = "0") int pendingPage) {
        feedbackService.block(id);
        return redirectAudit(flaggedPage, pendingPage);
    }

    @PostMapping("/words")
    public String addWord(@Valid @ModelAttribute AddProfanityWordRequest request,
                          BindingResult bindingResult,
                          @RequestParam(defaultValue = "") String search,
                          @RequestParam(defaultValue = "") String language,
                          @RequestParam(defaultValue = "") String source,
                          @RequestParam(defaultValue = "") String activeFilter,
                          @RequestParam(defaultValue = "0") int wordsPage,
                          Model model) {
        if (bindingResult.hasErrors()) {
            populateWordsModel(model, search, language, source, activeFilter, wordsPage);
            model.addAttribute("tab", "words");
            return "admin/profanity";
        }
        managementService.add(request.word(), request.language(), WordSource.MANUAL);
        return redirectWords(search, language, source, activeFilter, wordsPage);
    }

    @PostMapping("/words/activate")
    public String activate(@RequestParam String word,
                           @RequestParam(defaultValue = "") String search,
                           @RequestParam(defaultValue = "") String language,
                           @RequestParam(defaultValue = "") String source,
                           @RequestParam(defaultValue = "") String activeFilter,
                           @RequestParam(defaultValue = "0") int wordsPage) {
        managementService.activate(word);
        return redirectWords(search, language, source, activeFilter, wordsPage);
    }

    @PostMapping("/words/deactivate")
    public String deactivate(@RequestParam String word,
                             @RequestParam(defaultValue = "") String search,
                             @RequestParam(defaultValue = "") String language,
                             @RequestParam(defaultValue = "") String source,
                             @RequestParam(defaultValue = "") String activeFilter,
                             @RequestParam(defaultValue = "0") int wordsPage) {
        managementService.deactivate(word);
        return redirectWords(search, language, source, activeFilter, wordsPage);
    }

    private String redirectAudit(int flaggedPage, int pendingPage) {
        return "redirect:/admin/profanity?tab=audit&flaggedPage=" + flaggedPage + "&pendingPage=" + pendingPage;
    }

    private String redirectWords(String search, String language, String source, String activeFilter, int wordsPage) {
        final String url = UriComponentsBuilder.fromPath("/admin/profanity")
                .queryParam("tab", "words")
                .queryParam("search", search)
                .queryParam("language", language)
                .queryParam("source", source)
                .queryParam("activeFilter", activeFilter)
                .queryParam("wordsPage", wordsPage)
                .encode()
                .toUriString();
        return "redirect:" + url;
    }

    private void populateWordsModel(Model model, String search, String language, String source,
                                    String activeFilter, int wordsPage) {
        final Language langFilter = language.isBlank() ? null : Language.valueOf(language);
        final WordSource sourceFilter = source.isBlank() ? null : WordSource.valueOf(source);
        final Boolean activeOnly = "active".equals(activeFilter) ? Boolean.TRUE
                : "inactive".equals(activeFilter) ? Boolean.FALSE : null;
        final Page<ProfanityWord> words =
                managementService.findAllPaged(search, langFilter, sourceFilter, activeOnly, wordsPage, WORDS_PAGE_SIZE);
        model.addAttribute("words", words);
        model.addAttribute("search", search);
        model.addAttribute("language", language);
        model.addAttribute("source", source);
        model.addAttribute("activeFilter", activeFilter);
        model.addAttribute("wordsPage", wordsPage);
        model.addAttribute("wordsPageStart", Math.max(0, wordsPage - 2));
        model.addAttribute("wordsPageEnd", Math.min(words.getTotalPages() - 1, wordsPage + 2));
        model.addAttribute("languages", Language.values());
        model.addAttribute("sources", WordSource.values());
    }

    private AuditRow toRow(NicknameAudit e) {
        return new AuditRow(
                e.getId(),
                e.getNickname(),
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
    ) {}
}
