package coffeeshout.patchnote.application;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.patchnote.domain.PatchNoteCategory;
import coffeeshout.patchnote.domain.PatchNoteErrorCode;
import coffeeshout.patchnote.domain.PatchNoteRepository;
import coffeeshout.patchnote.infra.persistence.PatchNoteEntity;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PatchNoteAdminService {

    private final Clock clock;
    private final PatchNoteRepository patchNoteRepository;

    @Transactional(readOnly = true)
    public List<AdminRow> findAll() {
        return patchNoteRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toRow)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminRow findById(Long id) {
        return patchNoteRepository.findById(id)
                .map(this::toRow)
                .orElseThrow(() -> new BusinessException(PatchNoteErrorCode.NOT_FOUND, PatchNoteErrorCode.NOT_FOUND.getMessage()));
    }

    @Transactional
    public Long create(PatchNoteCategory category, String title, String content) {
        final PatchNoteEntity entity = PatchNoteEntity.create(category, title, content);
        return patchNoteRepository.save(entity).getId();
    }

    @Transactional
    public void update(Long id, PatchNoteCategory category, String title, String content) {
        final PatchNoteEntity entity = patchNoteRepository.findById(id)
                .orElseThrow(() -> new BusinessException(PatchNoteErrorCode.NOT_FOUND, PatchNoteErrorCode.NOT_FOUND.getMessage()));
        entity.update(category, title, content);
    }

    @Transactional
    public void delete(Long id) {
        patchNoteRepository.deleteById(id);
    }

    private AdminRow toRow(PatchNoteEntity entity) {
        return new AdminRow(
                entity.getId(),
                entity.getCategory(),
                entity.getTitle(),
                entity.getContent(),
                toKst(entity.getCreatedAt()),
                toKst(entity.getUpdatedAt())
        );
    }

    private LocalDateTime toKst(Instant instant) {
        return LocalDateTime.ofInstant(instant, clock.getZone());
    }
}
