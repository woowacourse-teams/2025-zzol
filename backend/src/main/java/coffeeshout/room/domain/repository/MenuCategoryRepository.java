package coffeeshout.room.domain.repository;

import coffeeshout.room.domain.menu.MenuCategory;
import java.util.List;
import java.util.Optional;

public interface MenuCategoryRepository {

    List<MenuCategory> getAll();

    MenuCategory save(MenuCategory category);

    Optional<MenuCategory> findById(Long id);
}
