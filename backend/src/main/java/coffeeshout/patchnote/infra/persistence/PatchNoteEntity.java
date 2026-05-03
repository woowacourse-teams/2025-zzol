package coffeeshout.patchnote.infra.persistence;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.patchnote.domain.PatchNoteCategory;
import coffeeshout.patchnote.exception.PatchNoteErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "patch_note")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PatchNoteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PatchNoteCategory category;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public static PatchNoteEntity create(PatchNoteCategory category, String title, String content) {
        validateCategory(category);
        validateTitle(title);
        validateContent(content);
        final PatchNoteEntity entity = new PatchNoteEntity();
        entity.category = category;
        entity.title = title;
        entity.content = content;
        entity.createdAt = Instant.now();
        entity.updatedAt = entity.createdAt;
        return entity;
    }

    public void update(PatchNoteCategory category, String title, String content) {
        validateCategory(category);
        validateTitle(title);
        validateContent(content);
        this.category = category;
        this.title = title;
        this.content = content;
        this.updatedAt = Instant.now();
    }

    private static void validateCategory(PatchNoteCategory category) {
        if (category == null) {
            throw new BusinessException(PatchNoteErrorCode.INVALID_CATEGORY, PatchNoteErrorCode.INVALID_CATEGORY.getMessage());
        }
    }

    private static void validateTitle(String title) {
        if (title == null || title.isBlank() || title.length() > 100) {
            throw new BusinessException(PatchNoteErrorCode.INVALID_TITLE, PatchNoteErrorCode.INVALID_TITLE.getMessage());
        }
    }

    private static void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(PatchNoteErrorCode.INVALID_CONTENT, PatchNoteErrorCode.INVALID_CONTENT.getMessage());
        }
        if (content.length() > 5000) {
            throw new BusinessException(PatchNoteErrorCode.INVALID_CONTENT_LENGTH, PatchNoteErrorCode.INVALID_CONTENT_LENGTH.getMessage());
        }
    }
}
