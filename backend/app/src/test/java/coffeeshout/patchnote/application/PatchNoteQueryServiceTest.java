package coffeeshout.patchnote.application;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.ServiceTest;
import coffeeshout.patchnote.domain.PatchNoteCategory;
import coffeeshout.patchnote.infra.persistence.PatchNoteEntity;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PatchNoteQueryServiceTest extends ServiceTest {

    @Autowired
    private PatchNoteAdminService patchNoteAdminService;

    @Autowired
    private PatchNoteQueryService patchNoteQueryService;

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        void 모든_패치노트를_최신순으로_반환한다() {
            patchNoteAdminService.create(PatchNoteCategory.NOTICE, "첫 번째", "내용1");
            patchNoteAdminService.create(PatchNoteCategory.EVENT, "두 번째", "내용2");

            final List<PatchNoteEntity> result = patchNoteQueryService.findAll();

            assertThat(result).hasSize(2);
            assertThat(result.getFirst().getTitle()).isEqualTo("두 번째");
        }

        @Test
        void 패치노트가_없으면_빈_목록을_반환한다() {
            assertThat(patchNoteQueryService.findAll()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findLatest")
    class FindLatest {

        @Test
        void 가장_최신_패치노트를_반환한다() {
            patchNoteAdminService.create(PatchNoteCategory.NOTICE, "오래된 공지", "내용1");
            patchNoteAdminService.create(PatchNoteCategory.UPDATE, "최신 업데이트", "내용2");

            final Optional<PatchNoteEntity> result = patchNoteQueryService.findLatest();

            assertThat(result).isPresent();
            assertThat(result.get().getTitle()).isEqualTo("최신 업데이트");
        }

        @Test
        void 패치노트가_없으면_빈_Optional을_반환한다() {
            assertThat(patchNoteQueryService.findLatest()).isEmpty();
        }
    }
}
