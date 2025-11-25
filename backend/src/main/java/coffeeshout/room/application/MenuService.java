package coffeeshout.room.application;

import coffeeshout.room.domain.menu.ProvidedMenu;
import coffeeshout.room.domain.service.MenuQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuQueryService menuQueryService;

    public List<ProvidedMenu> getAll() {
        return menuQueryService.getAll();
    }

    public List<ProvidedMenu> getAllMenuByCategoryId(Long categoryId) {
        return menuQueryService.getAllByCategoryId(categoryId);
    }
}
