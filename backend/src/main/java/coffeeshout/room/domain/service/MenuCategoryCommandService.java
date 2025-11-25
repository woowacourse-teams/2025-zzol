package coffeeshout.room.domain.service;

import coffeeshout.room.domain.menu.MenuCategory;
import coffeeshout.room.domain.repository.MenuCategoryRepository;
import org.springframework.stereotype.Service;

@Service
public class MenuCategoryCommandService {

    private final MenuCategoryRepository menuCategoryRepository;

    public MenuCategoryCommandService(MenuCategoryRepository menuCategoryRepository) {
        this.menuCategoryRepository = menuCategoryRepository;
    }

    public void save(MenuCategory menuCategory) {
        menuCategoryRepository.save(menuCategory);
    }
}
