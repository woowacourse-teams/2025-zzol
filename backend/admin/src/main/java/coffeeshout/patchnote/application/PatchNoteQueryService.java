package coffeeshout.patchnote.application;

import coffeeshout.patchnote.domain.PatchNoteRepository;
import coffeeshout.patchnote.infra.persistence.PatchNoteEntity;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PatchNoteQueryService {

    private final PatchNoteRepository patchNoteRepository;

    @Transactional(readOnly = true)
    public List<PatchNoteEntity> findAll() {
        return patchNoteRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Optional<PatchNoteEntity> findLatest() {
        return patchNoteRepository.findFirstByOrderByCreatedAtDesc();
    }
}
