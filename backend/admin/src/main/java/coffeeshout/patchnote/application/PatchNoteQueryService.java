package coffeeshout.patchnote.application;

import coffeeshout.patchnote.infra.persistence.PatchNoteEntity;
import coffeeshout.patchnote.infra.persistence.PatchNoteJpaRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PatchNoteQueryService {

    private final PatchNoteJpaRepository patchNoteJpaRepository;

    @Transactional(readOnly = true)
    public List<PatchNoteEntity> findAll() {
        return patchNoteJpaRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Optional<PatchNoteEntity> findLatest() {
        return patchNoteJpaRepository.findFirstByOrderByCreatedAtDesc();
    }
}
