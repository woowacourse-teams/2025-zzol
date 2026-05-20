package coffeeshout.patchnote.ui;

import coffeeshout.patchnote.application.PatchNoteQueryService;
import coffeeshout.patchnote.ui.response.PatchNoteResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/patch-notes")
@RequiredArgsConstructor
public class PatchNoteController implements PatchNoteApi {

    private final PatchNoteQueryService patchNoteQueryService;

    @GetMapping
    public ResponseEntity<List<PatchNoteResponse>> findAll() {
        final List<PatchNoteResponse> responses = patchNoteQueryService.findAll().stream()
                .map(PatchNoteResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/latest")
    public ResponseEntity<PatchNoteResponse> findLatest() {
        return patchNoteQueryService.findLatest()
                .map(PatchNoteResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
