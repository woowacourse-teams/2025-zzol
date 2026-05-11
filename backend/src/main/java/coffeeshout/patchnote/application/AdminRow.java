package coffeeshout.patchnote.application;

import coffeeshout.patchnote.domain.PatchNoteCategory;
import java.time.LocalDateTime;

public record AdminRow(
        Long id,
        PatchNoteCategory category,
        String title,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
