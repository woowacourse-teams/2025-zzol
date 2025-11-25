package coffeeshout.room.application;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.fixture.MenuCategoryFixture;
import coffeeshout.global.ServiceTest;
import coffeeshout.room.domain.menu.MenuCategory;
import coffeeshout.room.domain.repository.MenuCategoryRepository;
import coffeeshout.room.domain.service.MenuCategoryQueryService;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

class MenuCategoryServiceTest extends ServiceTest {

    @Autowired
    private MenuCategoryRepository menuCategoryRepository;

    @Autowired
    private MenuCategoryQueryService menuCategoryQueryService;

    @Autowired
    private MenuCategoryService menuCategoryService;

    private MenuCategory 커피;
    private MenuCategory 티;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(menuCategoryRepository, "menuCategories", new ConcurrentHashMap<>());
        ReflectionTestUtils.setField(menuCategoryRepository, "idGenerator", new AtomicLong(1));
        커피 = menuCategoryRepository.save(MenuCategoryFixture.커피());
        티 = menuCategoryRepository.save(MenuCategoryFixture.티());
    }

    @Test
    @DisplayName("모든 메뉴 카테고리를 조회한다")
    void 모든_메뉴_카테고리를_조회한다() {
        // given

        // when
        List<MenuCategory> result = menuCategoryService.getAll();

        // then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(커피, 티);
    }
}
