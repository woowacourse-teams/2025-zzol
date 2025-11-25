package coffeeshout.room.domain.menu;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MenuCategoryTest {

    @Test
    @DisplayName("메뉴 카테고리를 생성한다")
    void 메뉴_카테고리를_생성한다() {
        // given
        Long id = 1L;
        String name = "커피";
        String imageUrl = "coffee.jpg";

        // when
        MenuCategory menuCategory = new MenuCategory(id, name, imageUrl);

        // then
        assertThat(menuCategory.getId()).isEqualTo(id);
        assertThat(menuCategory.getName()).isEqualTo(name);
        assertThat(menuCategory.getImageUrl()).isEqualTo(imageUrl);
    }
}
