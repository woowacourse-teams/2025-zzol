package coffeeshout.room.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.fixture.MenuFixture;
import coffeeshout.global.ServiceTest;
import coffeeshout.global.exception.custom.NotExistElementException;
import coffeeshout.room.domain.menu.ProvidedMenu;
import coffeeshout.room.domain.repository.MenuRepository;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

class MenuQueryServiceTest extends ServiceTest {

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private MenuQueryService menuQueryService;

    private ProvidedMenu 아메리카노;
    private ProvidedMenu 라떼;
    private ProvidedMenu 아이스티;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(menuRepository, "menus", new ConcurrentHashMap<>());
        ReflectionTestUtils.setField(menuRepository, "idGenerator", new AtomicLong(1));

        아메리카노 = menuRepository.save((ProvidedMenu) MenuFixture.아메리카노());
        라떼 = menuRepository.save((ProvidedMenu) MenuFixture.라떼());
        아이스티 = menuRepository.save((ProvidedMenu) MenuFixture.아이스티());
    }

    @Test
    @DisplayName("ID로 메뉴를 조회한다")
    void ID로_메뉴를_조회한다() {
        // given
        Long menuId = 1L;

        // when
        ProvidedMenu result = menuQueryService.getById(menuId);

        // then
        assertThat(result).isEqualTo(아메리카노);
    }

    @Test
    @DisplayName("존재하지 않는 ID로 메뉴 조회시 예외가 발생한다")
    void 존재하지_않는_ID로_메뉴_조회시_예외가_발생한다() {
        // given
        Long nonExistentMenuId = 999L;

        // when & then
        assertThatThrownBy(() -> menuQueryService.getById(nonExistentMenuId))
                .isInstanceOf(NotExistElementException.class)
                .hasMessage("메뉴가 존재하지 않습니다.");
    }

    @Test
    @DisplayName("ID로 메뉴를 Optional로 조회한다")
    void ID로_메뉴를_Optional로_조회한다() {
        // given
        Long menuId = 1L;

        // when
        Optional<ProvidedMenu> result = menuQueryService.findById(menuId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(아메리카노);
    }

    @Test
    @DisplayName("모든 메뉴를 조회한다")
    void 모든_메뉴를_조회한다() {
        // given

        // when
        List<ProvidedMenu> result = menuQueryService.getAll();

        // then
        assertThat(result).hasSize(3);
        assertThat(result).containsExactly(아메리카노, 라떼, 아이스티);
    }

    @Test
    @DisplayName("카테고리 ID로 해당 카테고리의 메뉴들을 조회한다")
    void 카테고리_ID로_해당_카테고리의_메뉴들을_조회한다() {
        // given
        Long categoryId = 1L;

        // when
        List<ProvidedMenu> result = menuQueryService.getAllByCategoryId(categoryId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(아메리카노, 라떼);
    }

    @Test
    @DisplayName("존재하지 않는 카테고리 ID로 조회시 빈 리스트를 반환한다")
    void 존재하지_않는_카테고리_ID로_조회시_빈_리스트를_반환한다() {
        // given
        Long nonExistentCategoryId = 999L;

        // when
        List<ProvidedMenu> result = menuQueryService.getAllByCategoryId(nonExistentCategoryId);

        // then
        assertThat(result).isEmpty();
    }
}
