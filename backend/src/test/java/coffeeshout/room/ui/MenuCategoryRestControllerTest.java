package coffeeshout.room.ui;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import coffeeshout.fixture.MenuCategoryFixture;
import coffeeshout.fixture.MenuFixture;
import coffeeshout.global.config.IntegrationTestConfig;
import coffeeshout.room.domain.menu.MenuCategory;
import coffeeshout.room.domain.menu.ProvidedMenu;
import coffeeshout.room.domain.repository.MenuCategoryRepository;
import coffeeshout.room.domain.repository.MenuRepository;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@Import({IntegrationTestConfig.class})
@AutoConfigureMockMvc
class MenuCategoryRestControllerTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private MenuCategoryRepository menuCategoryRepository;

    @Autowired
    private MenuRepository menuRepository;

    private MenuCategory 커피;
    private MenuCategory 티;
    private MenuCategory 에이드;
    private ProvidedMenu 아메리카노;
    private ProvidedMenu 라떼;
    private ProvidedMenu 아이스티;

    @BeforeEach
    void setUp() {
        // Repository 직접 초기화
        ReflectionTestUtils.setField(menuCategoryRepository, "menuCategories", new ConcurrentHashMap<>());
        ReflectionTestUtils.setField(menuCategoryRepository, "idGenerator", new AtomicLong(1));
        ReflectionTestUtils.setField(menuRepository, "menus", new ConcurrentHashMap<>());
        ReflectionTestUtils.setField(menuRepository, "idGenerator", new AtomicLong(1));

        // 테스트 데이터 직접 저장
        커피 = menuCategoryRepository.save(MenuCategoryFixture.커피());
        티 = menuCategoryRepository.save(MenuCategoryFixture.티());
        에이드 = menuCategoryRepository.save(MenuCategoryFixture.에이드());
        
        아메리카노 = menuRepository.save((ProvidedMenu) MenuFixture.아메리카노());
        라떼 = menuRepository.save((ProvidedMenu) MenuFixture.라떼());
        아이스티 = menuRepository.save((ProvidedMenu) MenuFixture.아이스티());
    }

    @Test
    @DisplayName("모든 메뉴 카테고리를 조회한다")
    void 모든_메뉴_카테고리를_조회한다() throws Exception {
        // when & then
        mockMvc.perform(get("/menu-categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.size()").value(3))
                .andExpect(jsonPath("$[0].id").value(커피.getId()))
                .andExpect(jsonPath("$[0].name").value("커피"))
                .andExpect(jsonPath("$[0].imageUrl").value("커피.jpg"))
                .andExpect(jsonPath("$[1].id").value(티.getId()))
                .andExpect(jsonPath("$[1].name").value("티"))
                .andExpect(jsonPath("$[1].imageUrl").value("티.jpg"))
                .andExpect(jsonPath("$[2].id").value(에이드.getId()))
                .andExpect(jsonPath("$[2].name").value("에이드"))
                .andExpect(jsonPath("$[2].imageUrl").value("에이드.jpg"));
    }

    @Test
    @DisplayName("특정 카테고리의 메뉴를 조회한다 - 커피 카테고리")
    void 특정_카테고리의_메뉴를_조회한다_커피_카테고리() throws Exception {
        // given
        Long 커피카테고리Id = 커피.getId();

        // when & then
        mockMvc.perform(get("/menu-categories/{categoryId}/menus", 커피카테고리Id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].id").value(아메리카노.getId()))
                .andExpect(jsonPath("$[0].name").value("아메리카노"))
                .andExpect(jsonPath("$[0].temperatureAvailability").value("BOTH"))
                .andExpect(jsonPath("$[1].id").value(라떼.getId()))
                .andExpect(jsonPath("$[1].name").value("라떼"))
                .andExpect(jsonPath("$[1].temperatureAvailability").value("BOTH"));
    }

    @Test
    @DisplayName("특정 카테고리의 메뉴를 조회한다 - 에이드 카테고리")
    void 특정_카테고리의_메뉴를_조회한다_에이드_카테고리() throws Exception {
        // given
        Long 에이드카테고리Id = 에이드.getId();

        // when & then
        mockMvc.perform(get("/menu-categories/{categoryId}/menus", 에이드카테고리Id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].id").value(아이스티.getId()))
                .andExpect(jsonPath("$[0].name").value("아이스티"))
                .andExpect(jsonPath("$[0].temperatureAvailability").value("BOTH"));
    }

    @Test
    @DisplayName("메뉴가 없는 카테고리 조회시 빈 배열을 반환한다")
    void 메뉴가_없는_카테고리_조회시_빈_배열을_반환한다() throws Exception {
        // given
        Long 티카테고리Id = 티.getId();

        // when & then
        mockMvc.perform(get("/menu-categories/{categoryId}/menus", 티카테고리Id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.size()").value(0));
    }

    @Test
    @DisplayName("존재하지 않는 카테고리 조회시 빈 배열을 반환한다")
    void 존재하지_않는_카테고리_조회시_빈_배열을_반환한다() throws Exception {
        // given
        Long nonExistentCategoryId = 999L;

        // when & then
        mockMvc.perform(get("/menu-categories/{categoryId}/menus", nonExistentCategoryId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.size()").value(0));
    }
}
