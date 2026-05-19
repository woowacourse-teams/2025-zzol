package coffeeshout.patchnote.ui.response;

import coffeeshout.patchnote.domain.PatchNoteCategory;
import coffeeshout.patchnote.infra.persistence.PatchNoteEntity;
import java.time.Instant;

public record PatchNoteResponse(
        Long id,
        PatchNoteCategory category,
        String categoryLabel,
        String title,
        String content,
        Instant createdAt,
        Instant updatedAt
) {
    public static PatchNoteResponse from(PatchNoteEntity entity) {
        return new PatchNoteResponse(
                entity.getId(),
                entity.getCategory(),
                entity.getCategory().label,
                entity.getTitle(),
                entity.getContent(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
