package coffeeshout.room.ui.response;

import coffeeshout.room.domain.menu.MenuCategory;
import java.util.List;

public record MenuCategoryResponse(
        Long id,
        String name,
        String imageUrl
) {

    public static MenuCategoryResponse from(MenuCategory menuCategory) {
        return new MenuCategoryResponse(menuCategory.getId(), menuCategory.getName(), menuCategory.getImageUrl());
    }
    
    public static List<MenuCategoryResponse> from(List<MenuCategory> menuCategories) {
        return menuCategories.stream()
                .map(MenuCategoryResponse::from)
                .toList();
    }
}
