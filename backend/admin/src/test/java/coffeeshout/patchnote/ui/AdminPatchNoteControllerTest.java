package coffeeshout.patchnote.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import coffeeshout.patchnote.application.AdminRow;
import coffeeshout.patchnote.application.PatchNoteAdminService;
import coffeeshout.patchnote.domain.PatchNoteCategory;
import coffeeshout.patchnote.ui.request.CreatePatchNoteRequest;
import coffeeshout.patchnote.ui.request.UpdatePatchNoteRequest;
import coffeeshout.patchnote.ui.response.PatchNoteAdminResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@DisplayName("AdminPatchNoteController")
@ExtendWith(MockitoExtension.class)
class AdminPatchNoteControllerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 6, 12, 0);

    @Mock
    private PatchNoteAdminService patchNoteAdminService;

    @InjectMocks
    private AdminPatchNoteController adminPatchNoteController;

    private static AdminRow row(Long id, String title) {
        return new AdminRow(id, PatchNoteCategory.UPDATE, title, "내용", NOW, NOW);
    }

    @Nested
    class list {

        @Test
        void 전체_목록을_돌려준다() {
            given(patchNoteAdminService.findAll()).willReturn(List.of(row(1L, "첫 번째"), row(2L, "두 번째")));

            assertThat(adminPatchNoteController.list())
                    .extracting(PatchNoteAdminResponse::title)
                    .containsExactly("첫 번째", "두 번째");
        }
    }

    @Nested
    class categories {

        @Test
        void 서버가_카테고리_선택지를_정한다() {
            // 프론트에 enum 을 복사해 두면 값이 늘 때 조용히 어긋난다.
            assertThat(adminPatchNoteController.categories()).containsExactly(PatchNoteCategory.values());
        }
    }

    @Nested
    class create {

        @Test
        void 생성_후_Location과_본문을_함께_돌려준다() {
            given(patchNoteAdminService.create(PatchNoteCategory.UPDATE, "제목", "내용"))
                    .willReturn(1L);
            given(patchNoteAdminService.findById(1L)).willReturn(row(1L, "제목"));

            final ResponseEntity<PatchNoteAdminResponse> response =
                    adminPatchNoteController.create(new CreatePatchNoteRequest(PatchNoteCategory.UPDATE, "제목", "내용"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getHeaders().getLocation()).hasToString("/admin/api/patch-notes/1");
            assertThat(response.getBody())
                    .isNotNull()
                    .extracting(PatchNoteAdminResponse::id)
                    .isEqualTo(1L);
        }
    }

    @Nested
    class update {

        @Test
        void 수정_후_갱신된_내용을_돌려준다() {
            given(patchNoteAdminService.findById(1L)).willReturn(row(1L, "바뀐 제목"));

            final PatchNoteAdminResponse response = adminPatchNoteController.update(
                    1L, new UpdatePatchNoteRequest(PatchNoteCategory.UPDATE, "바뀐 제목", "내용"));

            then(patchNoteAdminService).should().update(1L, PatchNoteCategory.UPDATE, "바뀐 제목", "내용");
            assertThat(response.title()).isEqualTo("바뀐 제목");
        }
    }

    @Nested
    class delete {

        @Test
        void 삭제를_위임한다() {
            adminPatchNoteController.delete(1L);

            then(patchNoteAdminService).should().delete(1L);
        }
    }
}
