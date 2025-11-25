package coffeeshout.room.ui;

import coffeeshout.room.application.MenuCategoryService;
import coffeeshout.room.application.MenuService;
import coffeeshout.room.domain.menu.MenuCategory;
import coffeeshout.room.domain.menu.ProvidedMenu;
import coffeeshout.room.ui.response.MenuCategoryResponse;
import coffeeshout.room.ui.response.SelectableMenuResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/menu-categories")
@RequiredArgsConstructor
public class MenuCategoryRestController implements MenuCategoryApi {

    private final MenuService menuService;
    private final MenuCategoryService menuCategoryService;

    @GetMapping
    public ResponseEntity<List<MenuCategoryResponse>> getAllMenuCategories() {
        final List<MenuCategory> menuCategories = menuCategoryService.getAll();
        return ResponseEntity.ok(MenuCategoryResponse.from(menuCategories));
    }

    @GetMapping("/{categoryId}/menus")
    public ResponseEntity<List<SelectableMenuResponse>> getMenusByCategory(@PathVariable("categoryId") Long categoryId) {
        final List<ProvidedMenu> menus = menuService.getAllMenuByCategoryId(categoryId);
        return ResponseEntity.ok(SelectableMenuResponse.from(menus));
    }
}
