package coffeeshout.room.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.fixture.MenuCategoryFixture;
import coffeeshout.global.ServiceTest;
import coffeeshout.room.domain.menu.MenuCategory;
import coffeeshout.room.domain.repository.MenuCategoryRepository;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

class MenuCategoryQueryServiceTest extends ServiceTest {

    @Autowired
    private MenuCategoryRepository menuCategoryRepository;

    @Autowired
    private MenuCategoryQueryService menuCategoryQueryService;

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
        List<MenuCategory> result = menuCategoryQueryService.getAll();

        // then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(커피, 티);
    }

    @Test
    @DisplayName("ID로 메뉴 카테고리를 조회한다")
    void ID로_메뉴_카테고리를_조회한다() {
        // given
        Long categoryId = 1L;

        // when
        MenuCategory result = menuCategoryQueryService.getById(categoryId);

        // then
        assertThat(result).isEqualTo(커피);
    }

    @Test
    @DisplayName("존재하지 않는 ID로 메뉴 카테고리 조회시 예외가 발생한다")
    void 존재하지_않는_ID로_메뉴_카테고리_조회시_예외가_발생한다() {
        // given
        Long nonExistentCategoryId = 999L;

        // when & then
        assertThatThrownBy(() -> menuCategoryQueryService.getById(nonExistentCategoryId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 메뉴 카테고리입니다.");
    }
}
