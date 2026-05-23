package coffeeshout.patchnote.domain;

import coffeeshout.patchnote.infra.persistence.PatchNoteEntity;
import java.util.List;
import java.util.Optional;

public interface PatchNoteRepository {

    PatchNoteEntity save(PatchNoteEntity entity);

    Optional<PatchNoteEntity> findById(Long id);

    void deleteById(Long id);

    List<PatchNoteEntity> findAllByOrderByCreatedAtDesc();

    Optional<PatchNoteEntity> findFirstByOrderByCreatedAtDesc();
}
