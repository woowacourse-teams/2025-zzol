package coffeeshout.patchnote.ui.request;

import coffeeshout.patchnote.domain.PatchNoteCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdatePatchNoteRequest(
        @NotNull PatchNoteCategory category,
        @NotBlank @Size(max = 100) String title,
        @NotBlank @Size(max = 5000) String content
) {
}
