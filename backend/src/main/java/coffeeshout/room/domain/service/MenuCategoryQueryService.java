package coffeeshout.room.domain.service;

import coffeeshout.room.domain.menu.MenuCategory;
import coffeeshout.room.domain.repository.MenuCategoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MenuCategoryQueryService {

    private final MenuCategoryRepository menuCategoryRepository;

    public List<MenuCategory> getAll() {
        return menuCategoryRepository.getAll();
    }

    public MenuCategory getById(Long id) {
        return menuCategoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 메뉴 카테고리입니다."));
    }
}
