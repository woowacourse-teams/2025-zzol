package coffeeshout.patchnote.application;

import static coffeeshout.global.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import coffeeshout.global.ServiceTest;
import coffeeshout.patchnote.domain.PatchNoteCategory;
import coffeeshout.patchnote.domain.PatchNoteErrorCode;
import coffeeshout.patchnote.infra.persistence.PatchNoteJpaRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PatchNoteAdminServiceTest extends ServiceTest {

    @Autowired
    private PatchNoteAdminService patchNoteAdminService;

    @Autowired
    private PatchNoteJpaRepository patchNoteJpaRepository;

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        void 패치노트를_저장하고_id를_반환한다() {
            final Long id = patchNoteAdminService.create(PatchNoteCategory.NOTICE, "공지 제목", "공지 본문입니다.");

            assertThat(patchNoteJpaRepository.findById(id)).isPresent();
        }

        @Test
        void 저장된_패치노트_필드가_일치한다() {
            final Long id = patchNoteAdminService.create(PatchNoteCategory.UPDATE, "업데이트 제목", "업데이트 내용");

            final var saved = patchNoteJpaRepository.findById(id).orElseThrow();
            assertSoftly(softly -> {
                softly.assertThat(saved.getCategory()).isEqualTo(PatchNoteCategory.UPDATE);
                softly.assertThat(saved.getTitle()).isEqualTo("업데이트 제목");
                softly.assertThat(saved.getContent()).isEqualTo("업데이트 내용");
            });
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        void 최신순으로_반환한다() {
            patchNoteAdminService.create(PatchNoteCategory.NOTICE, "첫 번째 공지", "내용1");
            patchNoteAdminService.create(PatchNoteCategory.EVENT, "두 번째 이벤트", "내용2");

            final List<AdminRow> result = patchNoteAdminService.findAll();

            assertThat(result.getFirst().title()).isEqualTo("두 번째 이벤트");
        }

        @Test
        void 패치노트가_없으면_빈_목록을_반환한다() {
            assertThat(patchNoteAdminService.findAll()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        void id로_패치노트를_조회한다() {
            final Long id = patchNoteAdminService.create(PatchNoteCategory.MAINTENANCE, "점검 공지", "점검 예정입니다.");

            final AdminRow row = patchNoteAdminService.findById(id);

            assertSoftly(softly -> {
                softly.assertThat(row.category()).isEqualTo(PatchNoteCategory.MAINTENANCE);
                softly.assertThat(row.title()).isEqualTo("점검 공지");
            });
        }

        @Test
        void 존재하지_않는_id이면_예외가_발생한다() {
            assertCoffeeShoutException(
                    () -> patchNoteAdminService.findById(999L),
                    PatchNoteErrorCode.NOT_FOUND
            );
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        void 패치노트를_수정한다() {
            final Long id = patchNoteAdminService.create(PatchNoteCategory.NOTICE, "원래 제목", "원래 본문");

            patchNoteAdminService.update(id, PatchNoteCategory.EVENT, "수정 제목", "수정 본문");

            final var updated = patchNoteJpaRepository.findById(id).orElseThrow();
            assertSoftly(softly -> {
                softly.assertThat(updated.getCategory()).isEqualTo(PatchNoteCategory.EVENT);
                softly.assertThat(updated.getTitle()).isEqualTo("수정 제목");
                softly.assertThat(updated.getContent()).isEqualTo("수정 본문");
            });
        }

        @Test
        void 존재하지_않는_id이면_예외가_발생한다() {
            assertCoffeeShoutException(
                    () -> patchNoteAdminService.update(999L, PatchNoteCategory.NOTICE, "제목", "본문"),
                    PatchNoteErrorCode.NOT_FOUND
            );
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        void 패치노트를_삭제한다() {
            final Long id = patchNoteAdminService.create(PatchNoteCategory.NOTICE, "공지", "내용");

            patchNoteAdminService.delete(id);

            assertThat(patchNoteJpaRepository.findById(id)).isEmpty();
        }

        @Test
        void 존재하지_않는_id를_삭제해도_예외가_발생하지_않는다() {
            patchNoteAdminService.delete(999L);
        }
    }
}
