package coffeeshout.patchnote.infra.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface PatchNoteJpaRepository extends Repository<PatchNoteEntity, Long> {

    PatchNoteEntity save(PatchNoteEntity entity);

    Optional<PatchNoteEntity> findById(Long id);

    void deleteById(Long id);

    List<PatchNoteEntity> findAllByOrderByCreatedAtDesc();

    Optional<PatchNoteEntity> findFirstByOrderByCreatedAtDesc();
}
