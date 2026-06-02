package coffeeshout.patchnote.ui;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import coffeeshout.AdminModuleIntegrationTest;
import coffeeshout.patchnote.application.PatchNoteAdminService;
import coffeeshout.patchnote.domain.PatchNoteCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@DisplayName("PatchNoteController 통합 테스트")
class PatchNoteControllerTest extends AdminModuleIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    PatchNoteAdminService patchNoteAdminService;

    @Nested
    @DisplayName("GET /patch-notes")
    class FindAll {

        @Test
        void 패치노트_목록을_최신순으로_반환한다() throws Exception {
            patchNoteAdminService.create(PatchNoteCategory.NOTICE, "오래된 공지", "내용1");
            patchNoteAdminService.create(PatchNoteCategory.UPDATE, "최신 업데이트", "내용2");

            mockMvc.perform(get("/patch-notes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].title").value("최신 업데이트"))
                    .andExpect(jsonPath("$[0].category").value("UPDATE"))
                    .andExpect(jsonPath("$[0].categoryLabel").value("업데이트"))
                    .andExpect(jsonPath("$[0].content").value("내용2"))
                    .andExpect(jsonPath("$[1].title").value("오래된 공지"));
        }

        @Test
        void 패치노트가_없으면_빈_배열을_반환한다() throws Exception {
            mockMvc.perform(get("/patch-notes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /patch-notes/latest")
    class FindLatest {

        @Test
        void 최신_패치노트를_반환한다() throws Exception {
            patchNoteAdminService.create(PatchNoteCategory.NOTICE, "오래된 공지", "내용1");
            patchNoteAdminService.create(PatchNoteCategory.EVENT, "최신 이벤트", "이벤트 본문");

            mockMvc.perform(get("/patch-notes/latest"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("최신 이벤트"))
                    .andExpect(jsonPath("$.category").value("EVENT"))
                    .andExpect(jsonPath("$.categoryLabel").value("이벤트"))
                    .andExpect(jsonPath("$.content").value("이벤트 본문"));
        }

        @Test
        void 패치노트가_없으면_204를_반환한다() throws Exception {
            mockMvc.perform(get("/patch-notes/latest"))
                    .andExpect(status().isNoContent());
        }
    }
}
