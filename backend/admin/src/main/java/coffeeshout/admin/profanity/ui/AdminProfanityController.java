package coffeeshout.admin.profanity.ui;

import coffeeshout.admin.profanity.ui.request.AddProfanityWordRequest;
import coffeeshout.admin.profanity.ui.response.NicknameAuditResponse;
import coffeeshout.admin.profanity.ui.response.ProfanityWordResponse;
import coffeeshout.admin.support.PageResponse;
import coffeeshout.profanity.application.ProfanityAuditService;
import coffeeshout.profanity.application.ProfanityFeedbackService;
import coffeeshout.profanity.application.ProfanityWordManagementService;
import coffeeshout.profanity.domain.Language;
import coffeeshout.profanity.domain.WordSource;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 닉네임 검열 관리. 기존 Thymeleaf {@code ProfanityAdminController}의 REST 판이다.
 *
 * <p>탭 하나에 두 목록(FLAGGED, PENDING)을 함께 싣던 구조를 상태 파라미터 하나로 폈다.
 * 화면이 탭을 두 번 부르면 되고, 서버는 어느 탭이 열려 있는지 알 필요가 없다.
 */
@RestController
@RequestMapping("/admin/api/profanity")
@Validated
@RequiredArgsConstructor
public class AdminProfanityController {

    private static final int AUDIT_PAGE_SIZE = 10;
    private static final int WORDS_PAGE_SIZE = 20;
    private static final Sort AUDITED_AT_DESC = Sort.by("auditedAt").descending();

    private final Clock clock;
    private final ProfanityAuditService auditService;
    private final ProfanityFeedbackService feedbackService;
    private final ProfanityWordManagementService managementService;

    @GetMapping("/audits")
    public PageResponse<NicknameAuditResponse> audits(
            @RequestParam NicknameAuditStatus status, @RequestParam(defaultValue = "0") @Min(0) int page) {
        return PageResponse.of(
                auditService.listByStatus(status, PageRequest.of(page, AUDIT_PAGE_SIZE, AUDITED_AT_DESC)),
                audit -> NicknameAuditResponse.from(audit, clock.getZone()));
    }

    @PostMapping("/audits/{id}/allow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void allow(@PathVariable Long id) {
        feedbackService.allow(id);
    }

    @PostMapping("/audits/{id}/block")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void block(@PathVariable Long id) {
        feedbackService.block(id);
    }

    @GetMapping("/words")
    public PageResponse<ProfanityWordResponse> words(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) Language language,
            @RequestParam(required = false) WordSource source,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") @Min(0) int page) {
        // 필터 enum 을 문자열로 받아 직접 파싱하던 부분이 사라졌다. 스프링이 변환하고,
        // 값이 어긋나면 400 으로 떨어진다. 전에는 조용히 null 이 되어 필터가 무시됐다.
        return PageResponse.of(
                managementService.findAllPaged(search, language, source, active, page, WORDS_PAGE_SIZE),
                ProfanityWordResponse::from);
    }

    @PostMapping("/words")
    @ResponseStatus(HttpStatus.CREATED)
    public void addWord(@Valid @RequestBody AddProfanityWordRequest request) {
        managementService.add(request.word(), request.language(), WordSource.MANUAL);
    }

    @PostMapping("/words/{word}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activate(@PathVariable String word) {
        managementService.activate(word);
    }

    @DeleteMapping("/words/{word}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable String word) {
        managementService.deactivate(word);
    }
}
