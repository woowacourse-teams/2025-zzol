package coffeeshout.room.domain.service;

import coffeeshout.global.exception.GlobalErrorCode;
import coffeeshout.global.exception.custom.NotExistElementException;
import coffeeshout.room.domain.menu.CustomMenu;
import coffeeshout.room.domain.menu.Menu;
import coffeeshout.room.domain.menu.ProvidedMenu;
import coffeeshout.room.domain.repository.MenuRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MenuCommandService {

    private final MenuRepository menuRepository;
    private final String defaultCategoryImage;

    public MenuCommandService(MenuRepository menuRepository,
                              @Value("${menu-category.default-image}") String defaultCategoryImage) {
        this.menuRepository = menuRepository;
        this.defaultCategoryImage = defaultCategoryImage;
    }

    public void save(ProvidedMenu menu) {
        menuRepository.save(menu);
    }

    public Menu convertMenu(long id, String customName) {
        if (id == 0) {
            return new CustomMenu(customName, defaultCategoryImage);
        }
        return menuRepository.findById(id)
                .orElseThrow(() -> new NotExistElementException(
                        GlobalErrorCode.NOT_EXIST, "메뉴가 존재하지 않습니다."));
    }
}
