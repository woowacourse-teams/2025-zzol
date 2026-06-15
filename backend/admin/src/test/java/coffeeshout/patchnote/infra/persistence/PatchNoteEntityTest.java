package coffeeshout.patchnote.infra.persistence;

import static coffeeshout.support.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import coffeeshout.patchnote.domain.PatchNoteCategory;
import coffeeshout.patchnote.domain.PatchNoteErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PatchNoteEntityTest {

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        void 필드가_정상적으로_설정된다() {
            final PatchNoteEntity entity = PatchNoteEntity.create(
                    PatchNoteCategory.NOTICE, "1.0.0 공지", "서버 점검 예정입니다.");

            assertSoftly(softly -> {
                softly.assertThat(entity.getCategory()).isEqualTo(PatchNoteCategory.NOTICE);
                softly.assertThat(entity.getTitle()).isEqualTo("1.0.0 공지");
                softly.assertThat(entity.getContent()).isEqualTo("서버 점검 예정입니다.");
                softly.assertThat(entity.getCreatedAt()).isNotNull();
                softly.assertThat(entity.getUpdatedAt()).isEqualTo(entity.getCreatedAt());
            });
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void 제목이_공백이면_예외가_발생한다(String title) {
            assertCoffeeShoutException(
                    () -> PatchNoteEntity.create(PatchNoteCategory.NOTICE, title, "본문입니다."),
                    PatchNoteErrorCode.INVALID_TITLE
            );
        }

        @Test
        void 제목이_100자를_초과하면_예외가_발생한다() {
            final String longTitle = "가".repeat(101);

            assertCoffeeShoutException(
                    () -> PatchNoteEntity.create(PatchNoteCategory.NOTICE, longTitle, "본문입니다."),
                    PatchNoteErrorCode.INVALID_TITLE
            );
        }

        @Test
        void 제목이_정확히_100자이면_정상_생성된다() {
            final String title = "가".repeat(100);

            final PatchNoteEntity entity = PatchNoteEntity.create(PatchNoteCategory.NOTICE, title, "본문입니다.");

            assertThat(entity.getTitle()).hasSize(100);
        }

        @Test
        void 카테고리가_null이면_예외가_발생한다() {
            assertCoffeeShoutException(
                    () -> PatchNoteEntity.create(null, "제목", "본문입니다."),
                    PatchNoteErrorCode.INVALID_CATEGORY
            );
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void 본문이_공백이면_예외가_발생한다(String content) {
            assertCoffeeShoutException(
                    () -> PatchNoteEntity.create(PatchNoteCategory.NOTICE, "제목", content),
                    PatchNoteErrorCode.INVALID_CONTENT
            );
        }

        @Test
        void 본문이_5000자를_초과하면_예외가_발생한다() {
            final String longContent = "가".repeat(5001);

            assertCoffeeShoutException(
                    () -> PatchNoteEntity.create(PatchNoteCategory.NOTICE, "제목", longContent),
                    PatchNoteErrorCode.INVALID_CONTENT_LENGTH
            );
        }

        @Test
        void 본문이_정확히_5000자이면_정상_생성된다() {
            final String content = "가".repeat(5000);

            final PatchNoteEntity entity = PatchNoteEntity.create(PatchNoteCategory.NOTICE, "제목", content);

            assertThat(entity.getContent()).hasSize(5000);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        void 필드가_모두_갱신된다() {
            final PatchNoteEntity entity = PatchNoteEntity.create(
                    PatchNoteCategory.NOTICE, "원래 제목", "원래 본문");

            entity.update(PatchNoteCategory.EVENT, "수정 제목", "수정 본문");

            assertSoftly(softly -> {
                softly.assertThat(entity.getCategory()).isEqualTo(PatchNoteCategory.EVENT);
                softly.assertThat(entity.getTitle()).isEqualTo("수정 제목");
                softly.assertThat(entity.getContent()).isEqualTo("수정 본문");
            });
        }

        @Test
        void updatedAt이_createdAt보다_이전이_아니다() {
            final PatchNoteEntity entity = PatchNoteEntity.create(
                    PatchNoteCategory.NOTICE, "제목", "본문");

            entity.update(PatchNoteCategory.UPDATE, "새 제목", "새 본문");

            assertThat(entity.getUpdatedAt()).isAfterOrEqualTo(entity.getCreatedAt());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void 제목이_공백이면_예외가_발생한다(String title) {
            final PatchNoteEntity entity = PatchNoteEntity.create(
                    PatchNoteCategory.NOTICE, "제목", "본문");

            assertCoffeeShoutException(
                    () -> entity.update(PatchNoteCategory.NOTICE, title, "본문"),
                    PatchNoteErrorCode.INVALID_TITLE
            );
        }

        @Test
        void 제목이_100자를_초과하면_예외가_발생한다() {
            final PatchNoteEntity entity = PatchNoteEntity.create(
                    PatchNoteCategory.NOTICE, "제목", "본문");
            final String longTitle = "가".repeat(101);

            assertCoffeeShoutException(
                    () -> entity.update(PatchNoteCategory.NOTICE, longTitle, "본문"),
                    PatchNoteErrorCode.INVALID_TITLE
            );
        }

        @Test
        void 카테고리가_null이면_예외가_발생한다() {
            final PatchNoteEntity entity = PatchNoteEntity.create(
                    PatchNoteCategory.NOTICE, "제목", "본문");

            assertCoffeeShoutException(
                    () -> entity.update(null, "제목", "본문"),
                    PatchNoteErrorCode.INVALID_CATEGORY
            );
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void 본문이_공백이면_예외가_발생한다(String content) {
            final PatchNoteEntity entity = PatchNoteEntity.create(
                    PatchNoteCategory.NOTICE, "제목", "본문");

            assertCoffeeShoutException(
                    () -> entity.update(PatchNoteCategory.NOTICE, "제목", content),
                    PatchNoteErrorCode.INVALID_CONTENT
            );
        }

        @Test
        void 본문이_5000자를_초과하면_예외가_발생한다() {
            final PatchNoteEntity entity = PatchNoteEntity.create(
                    PatchNoteCategory.NOTICE, "제목", "본문");
            final String longContent = "가".repeat(5001);

            assertCoffeeShoutException(
                    () -> entity.update(PatchNoteCategory.NOTICE, "제목", longContent),
                    PatchNoteErrorCode.INVALID_CONTENT_LENGTH
            );
        }
    }
}
