package coffeeshout.admin.profanity.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import coffeeshout.admin.profanity.ui.request.AddProfanityWordRequest;
import coffeeshout.fixture.ProfanityWordFixture;
import coffeeshout.profanity.application.ProfanityWordManagementService;
import coffeeshout.profanity.domain.Language;
import coffeeshout.profanity.domain.ProfanityWord;
import coffeeshout.profanity.domain.WordSource;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

class ProfanityWordAdminControllerTest {

    private ProfanityWordManagementService managementService;
    private ProfanityWordAdminController controller;
    private Model model;

    @BeforeEach
    void setUp() {
        managementService = mock(ProfanityWordManagementService.class);
        controller = new ProfanityWordAdminController(managementService);
        model = new ExtendedModelMap();
    }

    @Nested
    class list_활성_비속어_목록_조회 {

        @Test
        void 활성_비속어_목록을_모델에_담아_뷰를_반환한다() {
            List<ProfanityWord> words = List.of(ProfanityWordFixture.한국어_수동_욕설());
            given(managementService.findAllActive()).willReturn(words);

            String viewName = controller.list(model);

            assertSoftly(softly -> {
                softly.assertThat(viewName).isEqualTo("admin/profanity-words");
                softly.assertThat(model.asMap()).containsEntry("words", words);
                softly.assertThat(model.asMap()).containsKey("languages");
            });
        }

        @Test
        void 단어가_없으면_빈_목록을_모델에_담는다() {
            given(managementService.findAllActive()).willReturn(List.of());

            controller.list(model);

            assertThat((List<?>) model.asMap().get("words")).isEmpty();
        }
    }

    @Nested
    class add_단어_추가 {

        @Test
        void 서비스에_단어를_추가하고_목록으로_리다이렉트한다() {
            AddProfanityWordRequest request = new AddProfanityWordRequest("욕설", Language.KOREAN);

            String viewName = controller.add(request);

            then(managementService).should().add("욕설", Language.KOREAN, WordSource.MANUAL);
            assertThat(viewName).isEqualTo("redirect:/admin/profanity/words");
        }

        @Test
        void 영어_단어_추가_시_MANUAL_출처로_등록한다() {
            AddProfanityWordRequest request = new AddProfanityWordRequest("badword", Language.ENGLISH);

            controller.add(request);

            then(managementService).should().add("badword", Language.ENGLISH, WordSource.MANUAL);
        }
    }

    @Nested
    class deactivate_단어_비활성화 {

        @Test
        void 서비스에_비활성화를_요청하고_목록으로_리다이렉트한다() {
            String viewName = controller.deactivate("욕설");

            then(managementService).should().deactivate("욕설");
            assertThat(viewName).isEqualTo("redirect:/admin/profanity/words");
        }
    }
}
