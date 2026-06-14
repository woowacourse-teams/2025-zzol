package coffeeshout.patchnote.infra.persistence;

import coffeeshout.patchnote.domain.PatchNoteRepository;
import org.springframework.data.repository.Repository;

public interface PatchNoteJpaRepository extends Repository<PatchNoteEntity, Long>, PatchNoteRepository {
}
