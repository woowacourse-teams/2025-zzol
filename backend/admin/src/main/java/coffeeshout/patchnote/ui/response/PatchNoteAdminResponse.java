package coffeeshout.patchnote.ui.response;

import coffeeshout.patchnote.application.AdminRow;
import coffeeshout.patchnote.domain.PatchNoteCategory;
import java.time.LocalDateTime;

public record PatchNoteAdminResponse(
        Long id,
        PatchNoteCategory category,
        String title,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static PatchNoteAdminResponse from(AdminRow row) {
        return new PatchNoteAdminResponse(
                row.id(), row.category(), row.title(), row.content(), row.createdAt(), row.updatedAt());
    }
}
