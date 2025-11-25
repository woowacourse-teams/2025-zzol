package coffeeshout.room.domain.service;

import coffeeshout.global.exception.GlobalErrorCode;
import coffeeshout.global.exception.custom.NotExistElementException;
import coffeeshout.room.domain.menu.ProvidedMenu;
import coffeeshout.room.domain.repository.MenuRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class MenuQueryService {

    private final MenuRepository menuRepository;

    public MenuQueryService(MenuRepository menuRepository) {
        this.menuRepository = menuRepository;
    }

    public ProvidedMenu getById(Long menuId) {
        return findById(menuId)
                .orElseThrow(() -> new NotExistElementException(GlobalErrorCode.NOT_EXIST, "메뉴가 존재하지 않습니다."));
    }

    public Optional<ProvidedMenu> findById(Long menuId) {
        return menuRepository.findById(menuId);
    }

    public List<ProvidedMenu> getAll() {
        return menuRepository.findAll();
    }

    public List<ProvidedMenu> getAllByCategoryId(Long categoryId) {
        return menuRepository.findAll().stream()
                .filter(menu -> Objects.equals(menu.getMenuCategory().getId(), categoryId))
                .toList();
    }
}
