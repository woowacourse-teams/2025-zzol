package coffeeshout.patchnote.ui;

import coffeeshout.patchnote.application.PatchNoteAdminService;
import coffeeshout.patchnote.domain.PatchNoteCategory;
import coffeeshout.patchnote.ui.request.CreatePatchNoteRequest;
import coffeeshout.patchnote.ui.request.UpdatePatchNoteRequest;
import coffeeshout.patchnote.ui.response.PatchNoteAdminResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 패치노트 관리. 기존 Thymeleaf {@code PatchNoteAdminController}의 REST 판이다.
 *
 * <p>수정은 POST 가 아니라 PUT 이다. 폼이 PUT 을 못 보내서 POST 로 우회하던 제약이
 * SPA 에서는 없다.
 */
@RestController
@RequestMapping("/admin/api/patch-notes")
@RequiredArgsConstructor
public class AdminPatchNoteController {

    private final PatchNoteAdminService patchNoteAdminService;

    @GetMapping
    public List<PatchNoteAdminResponse> list() {
        return patchNoteAdminService.findAll().stream()
                .map(PatchNoteAdminResponse::from)
                .toList();
    }

    /** 화면의 카테고리 선택지를 서버가 정한다. 프론트에 enum 을 복사해 두면 어긋난다. */
    @GetMapping("/categories")
    public PatchNoteCategory[] categories() {
        return PatchNoteCategory.values();
    }

    @GetMapping("/{id}")
    public PatchNoteAdminResponse findById(@PathVariable Long id) {
        return PatchNoteAdminResponse.from(patchNoteAdminService.findById(id));
    }

    @PostMapping
    public ResponseEntity<PatchNoteAdminResponse> create(@Valid @RequestBody CreatePatchNoteRequest request) {
        final Long id = patchNoteAdminService.create(request.category(), request.title(), request.content());
        final URI location = UriComponentsBuilder.fromPath("/admin/api/patch-notes/{id}")
                .buildAndExpand(id)
                .toUri();
        return ResponseEntity.created(location).body(PatchNoteAdminResponse.from(patchNoteAdminService.findById(id)));
    }

    @PutMapping("/{id}")
    public PatchNoteAdminResponse update(@PathVariable Long id, @Valid @RequestBody UpdatePatchNoteRequest request) {
        patchNoteAdminService.update(id, request.category(), request.title(), request.content());
        return PatchNoteAdminResponse.from(patchNoteAdminService.findById(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        patchNoteAdminService.delete(id);
    }
}
