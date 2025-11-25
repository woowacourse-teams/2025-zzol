package coffeeshout.room.application;

import coffeeshout.room.domain.menu.MenuCategory;
import coffeeshout.room.domain.service.MenuCategoryQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MenuCategoryService {

    private final MenuCategoryQueryService menuCategoryQueryService;

    public List<MenuCategory> getAll() {
        return menuCategoryQueryService.getAll();
    }
}
