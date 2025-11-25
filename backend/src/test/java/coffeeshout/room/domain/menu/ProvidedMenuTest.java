package coffeeshout.room.domain.menu;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.fixture.MenuCategoryFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProvidedMenuTest {

    @Test
    @DisplayName("ProvidedMenu를 생성한다")
    void ProvidedMenu를_생성한다() {
        // given
        Long id = 1L;
        String name = "아메리카노";
        MenuCategory menuCategory = MenuCategoryFixture.커피();
        TemperatureAvailability temperatureAvailability = TemperatureAvailability.BOTH;

        // when
        ProvidedMenu providedMenu = new ProvidedMenu(id, name, menuCategory, temperatureAvailability);

        // then
        assertThat(providedMenu.getId()).isEqualTo(id);
        assertThat(providedMenu.getName()).isEqualTo(name);
        assertThat(providedMenu.getMenuCategory()).isEqualTo(menuCategory);
        assertThat(providedMenu.getTemperatureAvailability()).isEqualTo(temperatureAvailability);
    }

    @Test
    @DisplayName("카테고리의 이미지 URL을 반환한다")
    void 카테고리의_이미지_URL을_반환한다() {
        // given
        MenuCategory menuCategory = MenuCategoryFixture.커피();
        ProvidedMenu providedMenu = new ProvidedMenu(1L, "아메리카노", menuCategory, TemperatureAvailability.BOTH);

        // when
        String categoryImageUrl = providedMenu.getCategoryImageUrl();

        // then
        assertThat(categoryImageUrl).isEqualTo("커피.jpg");
    }
}
