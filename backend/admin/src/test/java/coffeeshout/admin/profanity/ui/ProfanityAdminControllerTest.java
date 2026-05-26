package coffeeshout.admin.profanity.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import coffeeshout.admin.profanity.ui.request.AddProfanityWordRequest;
import coffeeshout.fixture.ProfanityWordFixture;
import coffeeshout.profanity.application.ProfanityAuditService;
import coffeeshout.profanity.application.ProfanityFeedbackService;
import coffeeshout.profanity.application.ProfanityWordManagementService;
import coffeeshout.profanity.domain.Language;
import coffeeshout.profanity.domain.ProfanityWord;
import coffeeshout.profanity.domain.WordSource;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

class ProfanityAdminControllerTest {

    private ProfanityWordManagementService managementService;
    private ProfanityAdminController controller;
    private Model model;

    @BeforeEach
    void setUp() {
        managementService = mock(ProfanityWordManagementService.class);
        controller = new ProfanityAdminController(
                mock(ProfanityAuditService.class),
                mock(ProfanityFeedbackService.class),
                managementService
        );
        model = new ExtendedModelMap();
    }

    @Nested
    class list_활성_비속어_목록_조회 {

        @Test
        void 활성_비속어_목록을_모델에_담아_뷰를_반환한다() {
            List<ProfanityWord> words = List.of(ProfanityWordFixture.한국어_수동_욕설());
            Page<ProfanityWord> page = new PageImpl<>(words, PageRequest.of(0, 20), words.size());
            given(managementService.findAllPaged(anyString(), isNull(), isNull(), isNull(), anyInt(), anyInt()))
                    .willReturn(page);

            String viewName = controller.page("words", 0, 0, "", 0, "", "", "", model);

            assertSoftly(softly -> {
                softly.assertThat(viewName).isEqualTo("admin/profanity");
                softly.assertThat(model.asMap()).containsEntry("words", page);
                softly.assertThat(model.asMap()).containsKey("languages");
                softly.assertThat(model.asMap()).containsKey("sources");
                softly.assertThat(model.asMap()).containsEntry("tab", "words");
            });
        }

        @Test
        void 단어가_없으면_빈_페이지를_모델에_담는다() {
            Page<ProfanityWord> emptyPage = Page.empty();
            given(managementService.findAllPaged(anyString(), isNull(), isNull(), isNull(), anyInt(), anyInt()))
                    .willReturn(emptyPage);

            controller.page("words", 0, 0, "", 0, "", "", "", model);

            Page<?> result = (Page<?>) model.asMap().get("words");
            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    class add_단어_추가 {

        @Test
        void 서비스에_단어를_추가하고_단어_탭으로_리다이렉트한다() {
            AddProfanityWordRequest request = new AddProfanityWordRequest("욕설", Language.KOREAN);
            BindingResult bindingResult = mock(BindingResult.class);

            String viewName = controller.addWord(request, bindingResult, "", "", "", "", 0, model);

            then(managementService).should().add("욕설", Language.KOREAN, WordSource.MANUAL);
            assertThat(viewName).isEqualTo(
                    "redirect:/admin/profanity?tab=words&search=&language=&source=&activeFilter=&wordsPage=0");
        }

        @Test
        void 영어_단어_추가_시_MANUAL_출처로_등록한다() {
            AddProfanityWordRequest request = new AddProfanityWordRequest("badword", Language.ENGLISH);
            BindingResult bindingResult = mock(BindingResult.class);

            controller.addWord(request, bindingResult, "", "", "", "", 0, model);

            then(managementService).should().add("badword", Language.ENGLISH, WordSource.MANUAL);
        }
    }

    @Nested
    class deactivate_단어_비활성화 {

        @Test
        void 서비스에_비활성화를_요청하고_단어_탭으로_리다이렉트한다() {
            String viewName = controller.deactivate("욕설", "", "", "", "", 0);

            then(managementService).should().deactivate("욕설");
            assertThat(viewName).isEqualTo(
                    "redirect:/admin/profanity?tab=words&search=&language=&source=&activeFilter=&wordsPage=0");
        }

        @Test
        void 검색어에_한글이_포함되면_리다이렉트_URL이_인코딩된다() {
            String viewName = controller.deactivate("욕설", "한글검색", "", "", "", 0);

            assertThat(viewName).startsWith("redirect:");
            assertThat(viewName).doesNotContain("한글검색");
            assertThat(viewName).contains("search=");
        }
    }

    @Nested
    class activate_단어_활성화 {

        @Test
        void 서비스에_활성화를_요청하고_단어_탭으로_리다이렉트한다() {
            String viewName = controller.activate("욕설", "", "", "", "", 0);

            then(managementService).should().activate("욕설");
            assertThat(viewName).isEqualTo(
                    "redirect:/admin/profanity?tab=words&search=&language=&source=&activeFilter=&wordsPage=0");
        }
    }
}
